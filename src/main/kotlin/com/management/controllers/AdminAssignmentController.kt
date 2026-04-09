package com.management.controllers

import com.management.repositories.PasswordEntryRepository
import com.management.repositories.UserRepository
import com.management.services.AssignmentService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/passwords/{passwordEntryId}/assignments")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
class AdminAssignmentController(
    private val assignmentService: AssignmentService,
    private val passwordEntryRepository: PasswordEntryRepository,
    private val userRepository: UserRepository
) {

    @GetMapping
    fun assignmentPage(@PathVariable passwordEntryId: Long, model: Model): String {
        val entry = passwordEntryRepository.findById(passwordEntryId)
            .orElseThrow { RuntimeException("Password entry not found: $passwordEntryId") }

        val users = userRepository.findAll()
        val assignedUserIds = assignmentService.assignedUserIdsForEntry(passwordEntryId)

        model.addAttribute("entry", entry)
        model.addAttribute("users", users)
        model.addAttribute("assignedUserIds", assignedUserIds)
        return "admin/assignment-form"
    }

    @PostMapping
    fun assign(
        @PathVariable passwordEntryId: Long,
        @RequestParam userId: Long
    ): String {
        assignmentService.assign(passwordEntryId, userId)
        return "redirect:/admin/passwords/$passwordEntryId/assignments"
    }

    @PostMapping("/remove")
    fun unassign(
        @PathVariable passwordEntryId: Long,
        @RequestParam userId: Long
    ): String {
        assignmentService.unassign(passwordEntryId, userId)
        return "redirect:/admin/passwords/$passwordEntryId/assignments"
    }
}