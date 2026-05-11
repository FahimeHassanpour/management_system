package com.management.controllers

import com.management.repositories.CategoryRepository
import com.management.services.AssignmentService
import com.management.services.PasswordEntryService
import com.management.util.PasswordExpiry
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/my/passwords")
class UserPasswordController(
    private val assignmentService: AssignmentService,
    private val passwordEntryService: PasswordEntryService,
    private val categoryRepository: CategoryRepository
) {
    @GetMapping
    fun list(
        authentication: Authentication,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        model: Model
    ): String {

        val username = authentication.name
        val assignedIds = assignmentService.assignedEntryIdsForUsername(username)

        val normalizedCategoryId = categoryId?.takeIf { it != 0L }

        // Fetch from search (paged) then keep only entries assigned to this user.
        // Note: pagination here is over all matching entries; the visible page may
        // contain fewer rows than `size` because of the assignment filter.
        val pageResult = passwordEntryService.list(
            query,
            normalizedCategoryId,
            page,
            size
        )

        val filteredEntries = pageResult.content.filter {
            it.id in assignedIds
        }

        model.addAttribute("entries", filteredEntries)
        model.addAttribute("currentPage", page)
        model.addAttribute("totalPages", pageResult.totalPages)
        model.addAttribute("totalElements", pageResult.totalElements)
        model.addAttribute("size", size)
        model.addAttribute("expiryDueIdStrings", PasswordExpiry.dueOrExpiredIdStrings(filteredEntries))

        model.addAttribute("query", query ?: "")
        model.addAttribute("categories", categoryRepository.findAll())
        model.addAttribute("categoryId", normalizedCategoryId)

        return "user/password-list"
    }

    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: Long,
        authentication: Authentication,
        model: Model
    ): String {
        val username = authentication.name
        if (!assignmentService.isAssignedToUser(id, username)) {
            throw AccessDeniedException("You are not allowed to view this password.")
        }

        model.addAttribute("entry", passwordEntryService.getById(id))
        return "user/password-detail"
    }
}
