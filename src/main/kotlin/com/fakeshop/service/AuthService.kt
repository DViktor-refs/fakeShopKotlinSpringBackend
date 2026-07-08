package com.fakeshop.service

import com.fakeshop.dto.LoginRequest
import com.fakeshop.dto.LoginResponse
import com.fakeshop.repository.UserRepository
import com.fakeshop.security.JwtProperties
import com.fakeshop.security.JwtService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
) {

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByUsername(request.username)
            ?: throw BadCredentialsException("Hibás felhasználónév vagy jelszó")

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw BadCredentialsException("Hibás felhasználónév vagy jelszó")
        }

        val token = jwtService.generateToken(user.username, user.role)
        return LoginResponse(
            token = token,
            tokenType = "Bearer",
            expiresIn = jwtProperties.expirationMs / 1000,
            username = user.username,
            role = user.role,
        )
    }
}
