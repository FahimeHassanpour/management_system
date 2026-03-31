package com.management.services

import com.management.models.Role
import com.management.models.User
import com.management.repositories.RoleRepository
import com.management.repositories.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AdminUserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun listUsers(): List<User> = userRepository.findAll().sortedBy { it.username.lowercase() }

    fun listRoles(): List<Role> = roleRepository.findAll().sortedBy { it.name.uppercase() }

    fun createUser(username: String, email: String, rawPassword: String, roleName: String): User {
        if (userRepository.findByUsername(username).isPresent) {
            throw RuntimeException("Username already exists")
        }
        if (userRepository.findByEmail(email).isPresent) {
            throw RuntimeException("Email already exists")
        }

        val normalizedRole = roleName.trim().uppercase()
        val role = roleRepository.findByName(normalizedRole)
            .orElseGet { roleRepository.save(Role(name = normalizedRole)) }

        val encodedPassword = passwordEncoder.encode(rawPassword)
            ?: throw RuntimeException("Failed to encode user password")
        val user = User(
            username = username.trim(),
            email = email.trim(),
            password = encodedPassword,
            role = role
        )
        return userRepository.save(user)
    }

    fun updateUserInfo(userId: Long, username: String, email: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found: $userId") }

        user.username = username.trim()
        user.email = email.trim()
        userRepository.save(user)
    }

    fun updateUserRole(userId: Long, roleName: String) {
        val normalizedRole = roleName.trim().uppercase()
        if (normalizedRole !in setOf("USER", "MANAGER", "ADMIN")) {
            throw RuntimeException("Unsupported role: $roleName")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found: $userId") }

        val role = roleRepository.findByName(normalizedRole)
            .orElseGet { roleRepository.save(Role(name = normalizedRole)) }

        user.role = role
        userRepository.save(user)
    }
}
