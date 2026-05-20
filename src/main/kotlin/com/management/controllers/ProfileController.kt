package com.management.controllers

import com.management.repositories.UserRepository
import com.management.services.ProfileService
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths

@Controller
@RequestMapping("/profile")
class ProfileController(
    private val userRepository: UserRepository,
    private val profileService: ProfileService
) {

    @GetMapping
    fun profile(
        authentication: Authentication,
        @RequestParam(required = false) success: Boolean?,
        model: Model
    ): String {
        val user = userRepository.findByUsername(authentication.name).orElseThrow()

        model.addAttribute("user", user)
        model.addAttribute("initials", initialsFromName(user.fullName))
        if (success == true) {
            model.addAttribute("successMessage", "Profile updated successfully.")
        }
        return "profile/profile"
    }

    @PostMapping
    fun updateProfile(
        authentication: Authentication,
        @RequestParam username: String,
        @RequestParam fullName: String,
        @RequestParam(required = false) password: String?,
        @RequestParam(required = false) profileImage: MultipartFile?
    ): String {
        var fileName: String? = null

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

        profileService.updateProfile(
            authentication.name,
            username.trim(),
            fullName.trim(),
            password,
            fileName
        )

        return "redirect:/profile?success=true"
    }

    private fun initialsFromName(fullName: String): String {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "?"
        if (parts.size == 1) return parts[0].take(2).uppercase()
        return (parts.first().first().toString() + parts.last().first()).uppercase()
    }
}
