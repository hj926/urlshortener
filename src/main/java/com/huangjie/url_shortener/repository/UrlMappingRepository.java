package com.huangjie.url_shortener.repository;

import com.huangjie.url_shortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
    Optional<UrlMapping> findByLongUrlHashAndExpireAtIsNull(String longUrlHash);
    Optional<UrlMapping> findByShortCode(String shortCode);
}
