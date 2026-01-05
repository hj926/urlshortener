package com.huangjie.url_shortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "url_mapping", indexes = {
                @Index(name = "idx_long_url_hash", columnList = "long_url_hash"),
                @Index(name = "idx_expire_at", columnList = "expire_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlMapping {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "short_code", unique = true, length = 16)
        private String shortCode;

        @Lob
        @Column(name = "long_url", nullable = false)
        private String longUrl;

        @Column(name = "long_url_hash", nullable = false, length = 32)
        private String longUrlHash;

        @Column(name = "created_at", nullable = false)
        private Instant createdAt;

        @Column(name = "expire_at")
        private Instant expireAt;

        @lombok.Builder.Default
        @Column(name = "hit_count", nullable = false)
        private Long hitCount = 0L;

}
