package com.management.services

import com.management.models.User
import com.management.models.Role
import com.management.repositories.UserRepository
import com.management.repositories.RoleRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun register(username: String, email: String, password: String): User {

        // check if user already exists by username or email
        if (userRepository.findByUsername(username).isPresent) {
            throw RuntimeException("Username already exists")
        }
        if (userRepository.findByEmail(email).isPresent) {
            throw RuntimeException("Email already exists")
        }

        // get default role USER (create it if missing to keep registration working)
        val role = roleRepository.findByName("USER")
            .orElseGet { roleRepository.save(Role(name = "USER")) }

        // create user
        val encodedPassword = passwordEncoder.encode(password)
            ?: throw RuntimeException("Password encoding failed")

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