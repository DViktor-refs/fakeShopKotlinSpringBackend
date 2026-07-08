package com.fakeshop.seed

import com.fakeshop.domain.AppUser
import com.fakeshop.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Induláskor létrehozza az 5 kamu felhasználót (user1..user5 / pass1..pass5),
 * ha még nincs felhasználó az adatbázisban. A jelszavak BCrypttel titkosítva kerülnek be.
 */
@Component
@Order(1)
class UserSeeder(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(UserSeeder::class.java)

    override fun run(args: ApplicationArguments?) {
        if (userRepository.count() > 0) {
            log.info("Már vannak felhasználók, a felhasználó-seedet kihagyom.")
            return
        }
        val users = (1..5).map { n ->
            AppUser(
                username = "user$n",
                email = "user$n@fakeshop.test",
                password = passwordEncoder.encode("pass$n"),
                role = if (n == 1) "ADMIN" else "USER",
            )
        }
        userRepository.saveAll(users)
        log.info("Létrehozva {} kamu felhasználó (user1=ADMIN, user2..user5=USER).", users.size)
    }
}
