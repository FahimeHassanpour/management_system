package com.management.services

import com.management.models.User
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

    fun register(name: String, email: String, password: String): User {

        // check if user already exists
        if (userRepository.findByEmail(email).isPresent) {
            throw RuntimeException("Email already exists")
        }

        // get default role USER
        val role = roleRepository.findByName("USER")
            .orElseThrow { RuntimeException("Role USER not found") }

        // create user
        val user = User(
            name = name,
            email = email,
            password = passwordEncoder.encode(password),
            role = role
        )

        return userRepository.save(user)
    }

    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }
}