package com.huangjie.url_shortener.controller;

import com.huangjie.url_shortener.exception.NotFoundException;
import com.huangjie.url_shortener.service.UrlShortenerService;
import com.huangjie.url_shortener.util.ShortCodeValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class RedirectController {

    private final UrlShortenerService service;

    public RedirectController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {

        // meet shortCode , get 404（dont search Redis/DB）
        if (!ShortCodeValidator.isValid(shortCode)) {
            throw new NotFoundException("Invalid short code: " + shortCode);
        }

        String longUrl = service.resolve(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(longUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302
    }
}
