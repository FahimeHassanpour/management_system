package com.management.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*



@Controller
class AuthController {
    @GetMapping("/login")
    fun showLoginPage(): String = "login"

    @GetMapping("/dashboard")
    fun dashboard(): String = "dashboard"
}