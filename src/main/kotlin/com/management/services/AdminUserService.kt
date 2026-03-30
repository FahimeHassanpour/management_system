package com.management.services

import com.management.models.Role
import com.management.models.User
import com.management.repositories.RoleRepository
import com.management.repositories.UserRepository
import org.springframework.stereotype.Service

@Service
class AdminUserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {
    fun listUsers(): List<User> = userRepository.findAll().sortedBy { it.username.lowercase() }

    fun listRoles(): List<Role> = roleRepository.findAll().sortedBy { it.name.uppercase() }

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
