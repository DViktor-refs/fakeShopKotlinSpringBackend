package com.fakeshop.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

/** A tokenből kiolvasott adatok. */
data class JwtPrincipal(
    val username: String,
    val role: String,
)

@Service
class JwtService(private val props: JwtProperties) {

    private val key: SecretKey = Keys.hmacShaKeyFor(props.secret.toByteArray(Charsets.UTF_8))

    /** Token generálása a felhasználónévre (subject) és a szerepkörre (role claim). */
    fun generateToken(username: String, role: String): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(Date(now))
            .expiration(Date(now + props.expirationMs))
            .signWith(key)
            .compact()
    }

    /**
     * Ellenőrzi a tokent (aláírás + lejárat), és visszaadja a benne lévő adatokat.
     * Érvénytelen token esetén null.
     */
    fun parse(token: String): JwtPrincipal? = try {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        val username = claims.subject ?: return null
        val role = claims.get("role", String::class.java) ?: "USER"
        JwtPrincipal(username, role)
    } catch (ex: Exception) {
        null
    }
}
