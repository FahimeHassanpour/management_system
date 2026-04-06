package com.management.controllers

import com.management.services.InvitationService
import com.management.services.UserService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class RegistrationController(
    private val invitationService: InvitationService,
    private val userService: UserService
) {

    private val strongPasswordRegex =
        Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$")

    @GetMapping("/register")
    fun show(@RequestParam token: String, model: Model): String {
        val invitation = invitationService.findValidByToken(token)
        if (invitation == null) {
            model.addAttribute("error", "Invalid or expired invitation.")
            return "register-error"
        }

        model.addAttribute("token", token)
        model.addAttribute("email", invitation.email)
        return "register"
    }

    @PostMapping("/register")
    fun submit(
        @RequestParam token: String,
        @RequestParam username: String,
        @RequestParam email: String,
        @RequestParam password: String,
        model: Model
    ): String {
        val invitation = invitationService.findValidByToken(token)
        if (invitation == null) {
            model.addAttribute("error", "Invalid or expired invitation.")
            return "register-error"
        }

        val normalizedEmail = email.trim()
        if (!normalizedEmail.equals(invitation.email, ignoreCase = true)) {
            model.addAttribute("error", "Email must match the invitation.")
            model.addAttribute("token", token)
            model.addAttribute("email", invitation.email)
            return "register"
        }

        if (!strongPasswordRegex.matches(password)) {
            model.addAttribute(
                "error",
                "Password must be at least 8 characters and include uppercase, lowercase, number, and symbol."
            )
            model.addAttribute("token", token)
            model.addAttribute("email", invitation.email)
            return "register"
        }

        return try {
            userService.register(username.trim(), normalizedEmail, password)
            invitationService.markUsed(invitation)
            "redirect:/login?registered"
        } catch (e: Exception) {
            model.addAttribute("error", e.message ?: "Registration failed")
            model.addAttribute("token", token)
            model.addAttribute("email", invitation.email)
            "register"
        }
    }
}