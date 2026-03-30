package com.management.services

import com.management.repositories.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(login: String): UserDetails {
        val normalizedLogin = login.trim()
        val user = userRepository.findByUsername(normalizedLogin)
            .orElseGet { userRepository.findByEmail(normalizedLogin).orElse(null) }
            ?: throw UsernameNotFoundException("User not found")

        val roleName = user.role?.name?.trim()?.uppercase() ?: "USER"
        val authority = if (roleName.startsWith("ROLE_")) roleName else "ROLE_$roleName"

        return org.springframework.security.core.userdetails.User(
            user.username,
            user.password,
            listOf(SimpleGrantedAuthority(authority))
        )
    }
}