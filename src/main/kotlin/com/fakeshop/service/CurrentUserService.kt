package com.fakeshop.service

import com.fakeshop.domain.AppUser
import com.fakeshop.repository.UserRepository
import com.fakeshop.web.ResourceNotFoundException
import org.springframework.stereotype.Service

@Service
class CurrentUserService(
    private val userRepository: UserRepository,
) {
    /** A tokenből jövő felhasználónév alapján betölti a felhasználót. */
    fun require(username: String): AppUser =
        userRepository.findByUsername(username)
            ?: throw ResourceNotFoundException("Nincs ilyen felhasználó: $username")
}
