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
    @GetMapping("/register")
    fun show(@RequestParam token: String, model: Model): String {
        val inv = invitationService.findValidByToken(token)
        if (inv == null) {
            model.addAttribute("error", "Invalid or expired invitation.")
            return "register-error"
        }
        model.addAttribute("token", token)
        model.addAttribute("email", inv.email)
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
        val inv = invitationService.findValidByToken(token)
        if (inv == null) {
            model.addAttribute("error", "Invalid or expired invitation.")
            return "register-error"
        }
        if (!email.trim().equals(inv.email, ignoreCase = true)) {
            model.addAttribute("error", "Email must match the invitation.")
            model.addAttribute("token", token)
            model.addAttribute("email", inv.email)
            return "register"
        }
        return try {
            userService.register(username.trim(), email.trim(), password)
            invitationService.markUsed(inv)
            "redirect:/login?registered"
        } catch (e: Exception) {
            model.addAttribute("error", e.message ?: "Registration failed")
            model.addAttribute("token", token)
            model.addAttribute("email", inv.email)
            "register"
        }
    }
}