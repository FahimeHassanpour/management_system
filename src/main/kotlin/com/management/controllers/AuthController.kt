package com.management.controllers

import com.management.dto.RegisterRequest
import com.management.services.UserService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*



@Controller
class AuthController(
    private val userService: UserService
)  {

    @GetMapping("/login")
    fun showLoginPage(): String = "login"

    @GetMapping("/auth/register")
    fun showRegisterPage(model: Model): String {
        model.addAttribute("registerRequest", RegisterRequest(
            username = "",
            email = "",
            password = ""
        ))
        return "register"
    }

    @PostMapping("/auth/register")
    fun register(
        @ModelAttribute registerRequest: RegisterRequest,
        model: Model
    ): String {
        return try {
            userService.register(
                registerRequest.username,
                registerRequest.email,
                registerRequest.password
            )
            "redirect:/login"
        } catch (e: Exception) {
            model.addAttribute("error", e.message)
            "register"
        }
    }

    @GetMapping("/dashboard")
    fun dashboard(): String = "dashboard"
}