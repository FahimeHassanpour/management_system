package com.management.config

import com.management.models.Role
import com.management.models.User
import com.management.repositories.RoleRepository
import com.management.repositories.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AdminBootstrapConfig {

    @Bean
    fun bootstrapFixedAdmin(
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner {
        return CommandLineRunner {
            val adminUsername = "admin"
            val adminEmail = "admin@local"
            val adminPassword = "admin123"

            val adminRole = roleRepository.findByName("ADMIN")
                .orElseGet { roleRepository.save(Role(name = "ADMIN")) }

            val adminUser = userRepository.findByUsername(adminUsername).orElseGet {
                User(
                    username = adminUsername,
                    email = adminEmail,
                    password = "",
                    role = adminRole
                )
            }

            adminUser.email = adminEmail
            val encoded = passwordEncoder.encode(adminPassword)
                ?: throw RuntimeException("Failed to encode admin password")
            adminUser.password = encoded
            adminUser.role = adminRole
            userRepository.save(adminUser)
        }
    }
}