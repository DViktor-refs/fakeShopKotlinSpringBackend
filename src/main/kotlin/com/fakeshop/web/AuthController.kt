package com.fakeshop.web

import com.fakeshop.dto.LoginRequest
import com.fakeshop.dto.LoginResponse
import com.fakeshop.dto.MeResponse
import com.fakeshop.service.AuthService
import com.fakeshop.service.CurrentUserService
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val currentUserService: CurrentUserService,
) {

    /** Bejelentkezés: felhasználónév + jelszó → JWT token. */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse =
        authService.login(request)

    /** A jelenleg bejelentkezett felhasználó adatai (tokent igényel). */
    @GetMapping("/me")
    fun me(authentication: Authentication): MeResponse {
        val user = currentUserService.require(authentication.name)
        return MeResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            role = user.role,
            createdAt = user.createdAt,
        )
    }
}
