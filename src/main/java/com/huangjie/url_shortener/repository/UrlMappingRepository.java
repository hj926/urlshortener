package com.huangjie.url_shortener.repository;

import com.huangjie.url_shortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
    Optional<UrlMapping> findByLongUrlHashAndExpireAtIsNull(String longUrlHash);
    Optional<UrlMapping> findByShortCode(String shortCode);

    @Modifying
    @Transactional
    @Query("update UrlMapping u set u.hitCount = u.hitCount + :delta where u.shortCode = :code")
    int addHits(@Param("code") String code, @Param("delta") long delta);
}
