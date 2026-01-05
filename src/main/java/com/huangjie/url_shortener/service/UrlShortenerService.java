package com.huangjie.url_shortener.service;

import com.huangjie.url_shortener.dto.ShortenRequest;
import com.huangjie.url_shortener.dto.ShortenResponse;
import com.huangjie.url_shortener.entity.UrlMapping;
import com.huangjie.url_shortener.repository.UrlMappingRepository;
import com.huangjie.url_shortener.util.Base62;
import com.huangjie.url_shortener.util.Hashing;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class UrlShortenerService {

    private final UrlMappingRepository repo;
    private final StringRedisTemplate redis;

    private static final String KEY_PREFIX = "url:code:";
    private static final String NULL_MARKER = "__NULL__";
    private static final Duration HIT_TTL = Duration.ofHours(24);
    private static final Duration MISS_TTL = Duration.ofSeconds(60);

    public UrlShortenerService(UrlMappingRepository repo, StringRedisTemplate redis) {
        this.repo = repo;
        this.redis = redis;
    }

    public ShortenResponse shorten(ShortenRequest req, String baseUrl) {
        String normalized = req.getLongUrl().trim();
        String hash = Hashing.sha256Hex(normalized).substring(0, 32);

        // 1) 去重（先做 expireAt = null 的永久链接）
        Optional<UrlMapping> existing = repo.findByLongUrlHashAndExpireAtIsNull(hash);
        if (existing.isPresent()) {
            UrlMapping m = existing.get();
            return new ShortenResponse(m.getShortCode(), baseUrl + "/" + m.getShortCode());
        }

        // 2) 生成短码（MVP：用 DB 自增 ID 的方式）
        UrlMapping created = UrlMapping.builder()
                .longUrl(normalized)
                .longUrlHash(hash)
                .createdAt(Instant.now())
                .expireAt(null)
                .build();

        // 先保存拿到自增 id
        created = repo.save(created);

        String shortCode = Base62.encode(created.getId());
        created.setShortCode(shortCode);
        repo.save(created);

        // 可选：把刚生成的 shortCode 也写进缓存（让第一次 redirect 也走缓存）
        redis.opsForValue().set(KEY_PREFIX + shortCode, created.getLongUrl(), HIT_TTL);

        return new ShortenResponse(shortCode, baseUrl + "/" + shortCode);
    }

    public String resolve(String shortCode) {
        String key = KEY_PREFIX + shortCode;

        // 1) 查 Redis
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) {
                throw new RuntimeException("Short code not found: " + shortCode);
            }
            return cached;
        }

        // 2) 查 DB
        UrlMapping m = repo.findByShortCode(shortCode).orElse(null);

        if (m == null) {
            // 缓存穿透保护：短 TTL 记空
            redis.opsForValue().set(key, NULL_MARKER, MISS_TTL);
            throw new RuntimeException("Short code not found: " + shortCode);
        }

        // 3) 回填缓存
        redis.opsForValue().set(key, m.getLongUrl(), HIT_TTL);
        return m.getLongUrl();
    }
}
