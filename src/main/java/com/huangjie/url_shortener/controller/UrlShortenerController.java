package com.huangjie.url_shortener.controller;

import com.huangjie.url_shortener.dto.ShortenRequest;
import com.huangjie.url_shortener.dto.ShortenResponse;
import com.huangjie.url_shortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ShortenResponse shorten(@Valid @RequestBody ShortenRequest req, HttpServletRequest httpReq) {
        String baseUrl = httpReq.getScheme() + "://" + httpReq.getServerName() + ":" + httpReq.getServerPort();
        return service.shorten(req, baseUrl);
    }
}
