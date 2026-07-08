package com.fakeshop.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "A username kötelező")
    val username: String = "",

    @field:NotBlank(message = "A password kötelező")
    val password: String = "",
)

data class LoginResponse(
    val token: String,
    val tokenType: String,
    val expiresIn: Long,
    val username: String,
    val role: String,
)

/** A jelenleg bejelentkezett felhasználó teljes, publikus adatai. */
data class MeResponse(
    val id: Long?,
    val username: String,
    val email: String,
    val role: String,
    val createdAt: java.time.OffsetDateTime,
)
