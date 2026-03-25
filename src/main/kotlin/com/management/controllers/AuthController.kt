package com.management.controllers

import com.management.dto.RegisterRequest
import com.management.models.User
import com.management.services.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<User> {
        val user = userService.register(
            request.name,
            request.email,
            request.password
        )
        return ResponseEntity.ok(user)
    }
}