package com.management.controllers

import com.management.services.AdminUserService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
class AdminUserController(
    private val adminUserService: AdminUserService
) {
    @GetMapping
    fun usersPage(model: Model): String {
        model.addAttribute("users", adminUserService.listUsers())
        model.addAttribute("roles", adminUserService.listRoles())
        return "admin/user-management"
    }


    @GetMapping("/new")
    fun legacyNewUserRedirect(): String = "redirect:/admin/invitations"

    @PostMapping("/{userId}/role")
    fun updateRole(
        @PathVariable userId: Long,
        @RequestParam roleName: String
    ): String {
        adminUserService.updateUserRole(userId, roleName)
        return "redirect:/admin/users"
    }

    @PostMapping("/{userId}/info")
    fun updateUserInfo(
        @PathVariable userId: Long,
        @RequestParam username: String,
        @RequestParam email: String
    ): String {
        adminUserService.updateUserInfo(userId, username, email)
        return "redirect:/admin/users"
    }
}
