package com.management.controllers

import com.management.repositories.UserRepository
import com.management.services.ProfileService
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.nio.file.Files
import java.nio.file.Paths

@Controller
@RequestMapping("/profile")
class ProfileController(
    private val userRepository: UserRepository,
    private val profileService: ProfileService,
    private val passwordEncoder: PasswordEncoder
) {

    @GetMapping
    fun profile(authentication: Authentication, model: Model): String {
        populateProfileModel(authentication, model)
        return "profile/profile"
    }

    @PostMapping
    fun updateProfile(
        authentication: Authentication,
        @RequestParam username: String,
        @RequestParam fullName: String,
        @RequestParam(required = false) profileImage: MultipartFile?,
        @RequestParam(required = false) currentPassword: String?,
        @RequestParam(required = false) newPassword: String?,
        @RequestParam(required = false) confirmPassword: String?,
        redirectAttributes: RedirectAttributes

    ): String {
        var fileName: String? = null

        val user = userRepository
            .findByUsername(authentication.name)
            .orElseThrow()

        if (!newPassword.isNullOrBlank()) {

            if (!passwordEncoder.matches(
                    currentPassword,
                    user.password
                )
            ) {
                return "redirect:/profile?error=currentPassword"
            }

            if (newPassword != confirmPassword) {
                return "redirect:/profile?error=passwordMismatch"
            }

            if (passwordEncoder.matches(
                    newPassword,
                    user.password
                )
            ) {
                return "redirect:/profile?error=samePassword"
            }
        }

        if (profileImage != null && !profileImage.isEmpty) {
            fileName = System.currentTimeMillis().toString() +
                "_" + profileImage.originalFilename

            val uploadPath = Paths.get(
                System.getProperty("user.dir"),
                "uploads",
                "profile-images"
            )

            Files.createDirectories(uploadPath)
            profileImage.transferTo(uploadPath.resolve(fileName).toFile())
        }

        return try {
            profileService.updateProfile(
                currentUsername = authentication.name,
                username = username.trim(),
                fullName = fullName.trim(),
                profileImage = fileName,
                currentPassword = currentPassword,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )

            val passwordChanged = !newPassword.isNullOrBlank() || !confirmPassword.isNullOrBlank()
            redirectAttributes.addFlashAttribute(
                "successMessage",
                if (passwordChanged) "Profile and password updated successfully." else "Profile updated successfully."
            )
            "redirect:/profile"
        } catch (ex: IllegalArgumentException) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.message)
            "redirect:/profile"
        }
    }

    private fun populateProfileModel(authentication: Authentication, model: Model) {
        val user = userRepository.findByUsername(authentication.name).orElseThrow()
        model.addAttribute("user", user)
        model.addAttribute("initials", initialsFromName(user.fullName))
    }

    private fun initialsFromName(fullName: String): String {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "?"
        if (parts.size == 1) return parts[0].take(2).uppercase()
        return (parts.first().first().toString() + parts.last().first()).uppercase()
    }
}
