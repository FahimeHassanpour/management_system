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
        profileImage: String?,
        currentPassword: String?,
        newPassword: String?,
        confirmPassword: String?
    ) {
        val user = userRepository.findByUsername(currentUsername).orElseThrow()

        val wantsPasswordChange = !newPassword.isNullOrBlank() || !confirmPassword.isNullOrBlank()

        if (wantsPasswordChange) {
            val current = currentPassword?.trim().orEmpty()
            val newPwd = newPassword?.trim().orEmpty()
            val confirm = confirmPassword?.trim().orEmpty()

            if (current.isBlank()) {
                throw IllegalArgumentException("Current password is required to change your password.")
            }

            if (!passwordEncoder.matches(current, user.password)) {
                throw IllegalArgumentException("Current password is incorrect.")
            }

            if (newPwd.isBlank()) {
                throw IllegalArgumentException("New password is required.")
            }

            if (newPwd != confirm) {
                throw IllegalArgumentException("New password and confirmation do not match.")
            }

            if (newPwd == current || passwordEncoder.matches(newPwd, user.password)) {
                throw IllegalArgumentException("New password must be different from current password.")
            }

            user.password = passwordEncoder.encode(newPwd)
                ?: throw IllegalStateException("Failed to encode password.")
        }

        user.username = username
        user.fullName = fullName

        if (profileImage != null) {
            user.profileImage = profileImage
        }

        userRepository.save(user)
    }
}
