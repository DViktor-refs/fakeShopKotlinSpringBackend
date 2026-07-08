package com.fakeshop.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtAuthFilter(jwtService: JwtService): JwtAuthFilter = JwtAuthFilter(jwtService)

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtAuthFilter: JwtAuthFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // Nyilvános végpontok:
                it.requestMatchers("/", "/actuator/**", "/api/auth/login").permitAll()
                // A termékek olvasása bárki számára engedélyezett:
                it.requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                // Barion szerver-szerver callback és a böngészőnek szánt visszatérő oldal:
                it.requestMatchers(HttpMethod.POST, "/api/payments/barion/callback").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/payments/barion/result").permitAll()
                // Értékelő emailből érkező, tokennel védett nyilvános oldalak:
                it.requestMatchers(HttpMethod.GET, "/review/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/review/**").permitAll()
                // A rendelés-állapot módosítása csak adminnak:
                it.requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasRole("ADMIN")
                // Minden más (kosár, rendelés, módosítás, /api/auth/me) tokent igényel:
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
