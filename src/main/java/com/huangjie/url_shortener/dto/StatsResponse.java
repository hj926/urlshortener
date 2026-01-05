package com.huangjie.url_shortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatsResponse {
    private String shortCode;
    private String longUrl;
    private long hitCount;
    private long pendingHits;
    private long totalHits;
}
