package com.management.controllers

import com.management.repositories.PasswordEntryRepository
import com.management.repositories.TeamPasswordAssignmentRepository
import com.management.repositories.TeamRepository
import com.management.repositories.UserRepository
import com.management.services.AssignmentService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException

@Controller
@RequestMapping("/admin/reports")
class ReportController(
    private val userRepository: UserRepository,
    private val assignmentService: AssignmentService,
    private val passwordEntryRepository: PasswordEntryRepository,
    private val teamRepository: TeamRepository,
    private val teamPasswordAssignmentRepository: TeamPasswordAssignmentRepository
) {

    @GetMapping("/user-access")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun page(model: Model): String {
        populateReportPage(model)
        return "admin/user-access-report"
    }

    @PostMapping("/user-access")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    fun report(
        @RequestParam scope: String,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) teamId: Long?,
        model: Model
    ): String {
        populateReportPage(model)

        return try {
            when (scope.lowercase()) {
                "user" -> generateUserReport(userId, model)
                "team" -> generateTeamReport(teamId, model)
                else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report type.")
            }
            "admin/user-access-report"
        } catch (ex: ResponseStatusException) {
            model.addAttribute("errorMessage", ex.reason ?: "Could not generate report.")
            model.addAttribute("reportScope", scope.lowercase())
            "admin/user-access-report"
        }
    }

    private fun generateUserReport(userId: Long?, model: Model) {
        val id = userId
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Select a user to generate the report."
            )

        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        val username = user.username.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Selected user has no username"
            )

        val accessibleIds =
            assignmentService.assignedEntryIdsForUsernameIncludingTeams(username)

        model.addAttribute("reportScope", "user")
        model.addAttribute("selectedUser", user)
        model.addAttribute("passwords", loadPasswords(accessibleIds))
    }

    private fun generateTeamReport(teamId: Long?, model: Model) {
        val id = teamId
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Select a team to generate the report."
            )

        val team = teamRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found") }

        val passwordIds =
            teamPasswordAssignmentRepository.findPasswordIdsByTeamIds(setOf(id))

        model.addAttribute("reportScope", "team")
        model.addAttribute("selectedTeam", team)
        model.addAttribute("passwords", loadPasswords(passwordIds))
    }

    private fun loadPasswords(ids: Set<Long>) =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            passwordEntryRepository.findAllById(ids)
                .sortedBy { it.title.lowercase() }
        }

    private fun populateReportPage(model: Model) {
        model.addAttribute("users", userRepository.findAll())
        model.addAttribute("teams", teamRepository.findAll())
        model.addAttribute("active", "reports")
    }
}
