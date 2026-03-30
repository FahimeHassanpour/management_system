package com.management.config

import com.management.models.Role
import com.management.models.User
import com.management.repositories.RoleRepository
import com.management.repositories.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AdminBootstrapConfig {

    @Bean
    fun bootstrapAdminUser(
        userRepository: UserRepository,
        roleRepository: RoleRepository,
        passwordEncoder: PasswordEncoder,
        @Value("\${app.bootstrap-admin.enabled:true}") enabled: Boolean,
        @Value("\${app.bootstrap-admin.username:admin}") adminUsername: String,
        @Value("\${app.bootstrap-admin.email:admin@local}") adminEmail: String,
        @Value("\${app.bootstrap-admin.password:admin123}") adminPassword: String
    ): CommandLineRunner {
        return CommandLineRunner {
            if (!enabled) return@CommandLineRunner

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

            // Keep admin bootstrap predictable for local development.
            adminUser.email = adminEmail
            adminUser.password = passwordEncoder.encode(adminPassword)
                ?: throw RuntimeException("Admin password encoding failed")
            adminUser.role = adminRole
            userRepository.save(adminUser)
        }
    }
}
