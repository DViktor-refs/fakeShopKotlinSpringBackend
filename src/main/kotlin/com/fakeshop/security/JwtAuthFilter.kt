package com.fakeshop.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Minden kérésnél megnézi az "Authorization: Bearer <token>" fejlécet.
 * Ha érvényes a token, beállítja a biztonsági kontextusba a felhasználót és a szerepkörét
 * (ROLE_USER vagy ROLE_ADMIN).
 */
class JwtAuthFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.substring(7)
            val principal = jwtService.parse(token)
            if (principal != null && SecurityContextHolder.getContext().authentication == null) {
                val authority = SimpleGrantedAuthority("ROLE_${principal.role}")
                val authentication = UsernamePasswordAuthenticationToken(
                    principal.username,
                    null,
                    listOf(authority),
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        filterChain.doFilter(request, response)
    }
}
