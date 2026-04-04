package com.management.services

import com.management.models.Role
import com.management.models.User
import com.management.repositories.RoleRepository
import com.management.repositories.UserRepository
import org.springframework.context.MessageSource
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class AdminUserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val messageSource: MessageSource
) {
    private fun msg(code: String): String =
        messageSource.getMessage(code, null, Locale.getDefault())
    fun listUsers(): List<User> = userRepository.findAll().sortedBy { it.username.lowercase() }

    fun listRoles(): List<Role> = roleRepository.findAll().sortedBy { it.name.uppercase() }

    fun updateUserInfo(userId: Long, username: String, email: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException(msg("error.user.notfound")) }

        user.username = username.trim()
        user.email = email.trim()
        userRepository.save(user)
    }

    fun updateUserRole(userId: Long, roleName: String) {
        val normalizedRole = roleName.trim().uppercase()
        if (normalizedRole !in setOf("USER", "MANAGER", "ADMIN")) {
            throw RuntimeException(msg("error.role.unsupported"))
        }

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException(msg("error.user.notfound")) }

        val role = roleRepository.findByName(normalizedRole)
            .orElseGet { roleRepository.save(Role(name = normalizedRole)) }

        user.role = role
        userRepository.save(user)
    }
}
