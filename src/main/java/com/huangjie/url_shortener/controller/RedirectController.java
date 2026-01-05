package com.huangjie.url_shortener.controller;

import com.huangjie.url_shortener.exception.NotFoundException;
import com.huangjie.url_shortener.service.UrlShortenerService;
import com.huangjie.url_shortener.util.ShortCodeValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import org.springframework.data.redis.core.StringRedisTemplate;

@RestController
public class RedirectController {

    private final UrlShortenerService service;

    private final StringRedisTemplate redis;
    private static final String HIT_KEY_PREFIX = "url:hit:";

    public RedirectController(UrlShortenerService service, StringRedisTemplate redis) {
        this.service = service;
        this.redis = redis;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {

        // meet shortCode , get 404（dont search Redis/DB）
        if (!ShortCodeValidator.isValid(shortCode)) {
            throw new NotFoundException("Invalid short code: " + shortCode);
        }

        String longUrl = service.resolve(shortCode);

        // PV 统计：只要成功重定向，就记一次
        redis.opsForValue().increment(HIT_KEY_PREFIX + shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(longUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302
    }
}
