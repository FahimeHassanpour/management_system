package com.management.controllers

import com.management.repositories.UserRepository
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AuthController(
    private val userRepository: UserRepository
) {
    @GetMapping("/login")
    fun showLoginPage(): String = "login"

    @GetMapping("/dashboard")
    fun dashboard(authentication: Authentication, model: Model): String {
        val user = userRepository.findByUsername(authentication.name).orElseThrow()
        model.addAttribute("sidebarUser", user)
        model.addAttribute("sidebarInitials", initialsFromName(user.fullName))
        return "dashboard"
    }

    private fun initialsFromName(fullName: String): String {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "?"
        if (parts.size == 1) return parts[0].take(2).uppercase()
        return (parts.first().first().toString() + parts.last().first()).uppercase()
    }
}
