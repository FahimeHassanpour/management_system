package com.management.controllers

import com.management.repositories.RoleRepository
import com.management.repositories.UserRepository
import com.management.services.AdminUserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
class AdminUserController(
    private val adminUserService: AdminUserService,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {
    @GetMapping
    fun usersPage(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        model: Model
    ): String {
        val safePage = if (page < 0) 0 else page
        val safeSize = if (size < 1) 10 else size

        val sort = Sort.by(Sort.Order.asc("username").ignoreCase())
        val pageResult = userRepository.findAll(PageRequest.of(safePage, safeSize, sort))

        model.addAttribute("users", pageResult.content)
        model.addAttribute("currentPage", pageResult.number)
        model.addAttribute("totalPages", pageResult.totalPages)
        model.addAttribute("totalElements", pageResult.totalElements)
        model.addAttribute("size", safeSize)
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


    @GetMapping("/{id}/edit")
    fun editUser(@PathVariable id: Long, model: Model): String {

        val user = userRepository.findById(id).orElseThrow()

        model.addAttribute("user", user)
        model.addAttribute("roles", roleRepository.findAll())

        return "admin/user-edit"
    }

    @PostMapping("/{id}/info")
    fun updateUserInfo(
        @PathVariable id: Long,
        @RequestParam username: String,
        @RequestParam email: String,
        @RequestParam fullName: String,
        @RequestParam roleId: Long
    ): String {

        val user = userRepository.findById(id).orElseThrow()

        val role = roleRepository.findById(roleId).orElseThrow()
        val normalizedRole = role.name.trim().uppercase()
        require(normalizedRole in setOf("USER", "MANAGER", "ADMIN")) {
            "Unsupported role"
        }

        user.username = username.trim()
        user.email = email.trim()
        user.fullName = fullName.trim()
        user.role = role

        userRepository.save(user)

        return "redirect:/admin/users"
    }
    @GetMapping("/admin/users")
    fun users(): String {
        return "admin-users :: content"
    }

}