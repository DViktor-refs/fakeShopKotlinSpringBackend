package com.fakeshop.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "fakeshop.jwt")
data class JwtProperties(
    /**
     * A token aláírásához használt titkos kulcs. HS-algoritmushoz legalább 32 bájt kell.
     * ÉLES környezetben FELTÉTLENÜL írd felül a JWT_SECRET környezeti változóval!
     */
    val secret: String = "fakeshop-default-dev-secret-change-me-please-0123456789-abcdef-xyz",
    /** A token élettartama ezredmásodpercben (alapból 24 óra). */
    val expirationMs: Long = 86_400_000,
)
