package com.huangjie.url_shortener.service;

import com.huangjie.url_shortener.repository.UrlMappingRepository;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.core.RedisCallback;


@Component
public class StatsFlushJob {

    private final StringRedisTemplate redis;
    private final UrlMappingRepository repo;

    private static final String HIT_KEY_PREFIX = "url:hit:";

    public StatsFlushJob(StringRedisTemplate redis, UrlMappingRepository repo) {
        this.redis = redis;
        this.repo = repo;
    }

    @Scheduled(fixedDelay = 30_000)
public void flushHitsToDb() {

    ScanOptions options = ScanOptions.scanOptions()
            .match(HIT_KEY_PREFIX + "*")
            .count(200)
            .build();

    redis.execute((RedisCallback<Void>) connection -> {
        try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8);

                String val = redis.opsForValue().getAndDelete(key);
                if (val == null) continue;

                long delta;
                try {
                    delta = Long.parseLong(val);
                } catch (NumberFormatException e) {
                    continue;
                }

                String code = key.substring(HIT_KEY_PREFIX.length());
                repo.addHits(code, delta);
            }
        }
        return null;
    });
}    


}
