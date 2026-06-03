package com.management.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import com.management.repositories.AssignmentRepository
import com.management.repositories.PasswordEntryRepository
import com.management.repositories.UserRepository
import com.management.services.AssignmentService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Controller
@RequestMapping("/admin/reports")
class ReportController(
    private val userRepository: UserRepository,
    private val assignmentRepository: AssignmentRepository,
    private val assignmentService: AssignmentService,
    private val passwordEntryRepository: PasswordEntryRepository
) {

    @GetMapping("/user-access")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun page(model: Model): String {

        model.addAttribute(
            "users",
            userRepository.findAll()
        )
        model.addAttribute(
            "active",
            "reports"
        )
        return "admin/user-access-report"
    }

    @PostMapping("/user-access")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun report(
        @RequestParam userId: Long,
        model: Model,
    ): String {
        val user =
            userRepository.findById(userId)
                .orElseThrow()

        val username = user.username ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Selected user has no username"
        )

        val accessibleIds = assignmentService.assignedEntryIdsForUsernameIncludingTeams(username)
        val passwords = if (accessibleIds.isEmpty()) {
            emptyList()
        } else {
            passwordEntryRepository.findAllById(accessibleIds)
                .sortedBy { it.title?.lowercase() ?: "" }
        }

        model.addAttribute(
            "users",
            userRepository.findAll())

        model.addAttribute(
            "selectedUser",
            user
        )

        model.addAttribute(
            "passwords",
            passwords

        )

        model.addAttribute(
            "active",
            "reports"
        )

        return "admin/user-access-report"

    }
}
