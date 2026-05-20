package com.management.services

import com.management.repositories.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class ProfileService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun updateProfile(
        currentUsername: String,
        username: String,
        fullName: String,
        password: String?,
        profileImage: String?
    ) {
        val user = userRepository.findByUsername(currentUsername).orElseThrow()

        user.username = username
        user.fullName = fullName

        if (!password.isNullOrBlank()) {
            user.password = passwordEncoder.encode(password)!!
        }

        // SAVE IMAGE
        if (profileImage != null) {
            user.profileImage = profileImage
        }

        userRepository.save(user)
    }
}