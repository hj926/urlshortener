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

import com.huangjie.url_shortener.exception.ExpiredException;
import com.huangjie.url_shortener.exception.NotFoundException;


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

        // 计算过期时间（Step22）
        Instant expireAt = null;
        if (req.getTtlSeconds() != null && req.getTtlSeconds() > 0) {
            expireAt = Instant.now().plusSeconds(req.getTtlSeconds());
        }

        // 去重：只对“永久链接”（expireAt == null）做去重，MVP策略
        if (expireAt == null) {
            Optional<UrlMapping> existing = repo.findByLongUrlHashAndExpireAtIsNull(hash);
            if (existing.isPresent()) {
                UrlMapping m = existing.get();
                return new ShortenResponse(m.getShortCode(), baseUrl + "/" + m.getShortCode());
            }
        }

        UrlMapping created = UrlMapping.builder()
                .longUrl(normalized)
                .longUrlHash(hash)
                .createdAt(Instant.now())
                .expireAt(expireAt)
                .build();

        // 先保存拿到自增 id
        created = repo.save(created);

        String shortCode = Base62.encode(created.getId());
        created.setShortCode(shortCode);
        repo.save(created);

        // 写缓存：TTL 跟随 expireAt（避免“过期了但缓存还在”）
        Duration cacheTtl = computeCacheTtl(created.getExpireAt());
        redis.opsForValue().set(KEY_PREFIX + shortCode, created.getLongUrl(), cacheTtl);

        return new ShortenResponse(shortCode, baseUrl + "/" + shortCode);
    }

    public String resolve(String shortCode) {
        String key = KEY_PREFIX + shortCode;

        // 1) 查 Redis
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) {
                throw new NotFoundException("Short code not found: " + shortCode);
            }
            return cached;
        }

        // 2) 查 DB
        UrlMapping m = repo.findByShortCode(shortCode).orElse(null);

        if (m == null) {
            redis.opsForValue().set(key, NULL_MARKER, MISS_TTL);
            throw new NotFoundException("Short code not found: " + shortCode);

        }

        // 过期判断（Step22）
        if (m.getExpireAt() != null && m.getExpireAt().isBefore(Instant.now())) {
            redis.opsForValue().set(key, NULL_MARKER, MISS_TTL);
            throw new ExpiredException("Short code expired: " + shortCode);

        }

        // 3) 回填缓存（TTL 跟随 expireAt）
        Duration ttl = computeCacheTtl(m.getExpireAt());
        redis.opsForValue().set(key, m.getLongUrl(), ttl);

        return m.getLongUrl();
    }

    private Duration computeCacheTtl(Instant expireAt) {
        if (expireAt == null) return HIT_TTL;

        long secondsLeft = expireAt.getEpochSecond() - Instant.now().getEpochSecond();
        if (secondsLeft <= 0) secondsLeft = 1;

        return Duration.ofSeconds(Math.min(secondsLeft, HIT_TTL.toSeconds()));
    }
}
