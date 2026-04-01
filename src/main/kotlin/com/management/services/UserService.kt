package com.management.services

import com.management.models.User
import com.management.models.Role
import com.management.repositories.UserRepository
import com.management.repositories.RoleRepository
import org.springframework.context.MessageSource
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val messageSource: MessageSource
) {

    private fun msg(code: String): String =
        messageSource.getMessage(code, null, Locale.getDefault())

    fun register(username: String, email: String, password: String): User {

        // check if user already exists by username or email
        if (userRepository.findByUsername(username).isPresent) {
            throw RuntimeException(msg("error.username.exists"))
        }
        if (userRepository.findByEmail(email).isPresent) {
            throw RuntimeException(msg("error.email.exists"))
        }

        // for invited members: always start as USER by default
        val role = roleRepository.findByName("USER")
            .orElseGet { roleRepository.save(Role(name = "USER")) }

        // create user
        val encodedPassword = passwordEncoder.encode(password)
            ?: throw RuntimeException(msg("error.user.password.encode.generic"))

        val user = User(
            username = username,
            email = email,
            password = encodedPassword,
            role = role
        )

        return userRepository.save(user)
    }

    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }

    fun findByUsername(username: String): User? {
        return userRepository.findByUsername(username).orElse(null)
    }
}