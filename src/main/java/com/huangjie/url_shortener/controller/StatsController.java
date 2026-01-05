package com.huangjie.url_shortener.controller;

import com.huangjie.url_shortener.dto.StatsResponse;
import com.huangjie.url_shortener.exception.NotFoundException;
import com.huangjie.url_shortener.service.UrlShortenerService;
import com.huangjie.url_shortener.util.ShortCodeValidator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class StatsController {

    private final UrlShortenerService service;

    public StatsController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/stats/{shortCode}")
    public StatsResponse stats(@PathVariable String shortCode) {
        if (!ShortCodeValidator.isValid(shortCode)) {
            throw new NotFoundException("Invalid short code: " + shortCode);
        }
        return service.stats(shortCode);
    }
}
