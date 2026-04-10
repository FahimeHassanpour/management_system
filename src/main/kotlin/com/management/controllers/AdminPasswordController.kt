package com.management.controllers

import com.management.dto.PasswordEntryRequest
import com.management.models.PasswordEntry
import com.management.repositories.CategoryRepository
import com.management.repositories.UserRepository
import com.management.services.AssignmentService
import com.management.services.PasswordEntryService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/passwords")
class AdminPasswordController(
    private val passwordEntryService: PasswordEntryService,
    private val categoryRepository: CategoryRepository,
    private val assignmentService: AssignmentService,
    private val userRepository: UserRepository
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun list(
        @RequestParam(required = false) query: String?,
        model: Model
    ): String {
        model.addAttribute("entries", passwordEntryService.list(query))
        model.addAttribute("query", query ?: "")
        return "admin/password-list"
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun createForm(model: Model): String {
        model.addAttribute("entryRequest", PasswordEntryRequest("", "", "", "", null, emptyList()))
        model.addAttribute("categories", categoryRepository.findAll())
        model.addAttribute("users", userRepository.findAll())
        model.addAttribute("selectedUserIds", emptySet<Long>())
        model.addAttribute("isEdit", false)
        return "admin/password-form"
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun create(@ModelAttribute entryRequest: PasswordEntryRequest): String {
        val entry = passwordEntryService.create(entryRequest)
        val entryId = entry.id ?: throw RuntimeException("Failed to create password entry")
        assignmentService.syncAssignments(entryId, entryRequest.userIds.toSet())
        return "redirect:/admin/passwords"
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun editForm(@PathVariable id: Long, model: Model): String {
        val entry: PasswordEntry = passwordEntryService.getById(id)
        model.addAttribute(
            "entryRequest",
            PasswordEntryRequest(
                title = entry.title,
                username = entry.username,
                password = entry.password,
                description = entry.description,
                categoryId = entry.category?.id,
                userIds = assignmentService.assignedUserIdsForEntry(id).toList()
            )
        )
        model.addAttribute("entryId", id)
        model.addAttribute("categories", categoryRepository.findAll())
        model.addAttribute("users", userRepository.findAll())
        model.addAttribute("selectedUserIds", assignmentService.assignedUserIdsForEntry(id))
        model.addAttribute("isEdit", true)
        return "admin/password-form"
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun update(@PathVariable id: Long, @ModelAttribute entryRequest: PasswordEntryRequest): String {
        passwordEntryService.update(id, entryRequest)
        assignmentService.syncAssignments(id, entryRequest.userIds.toSet())
        return "redirect:/admin/passwords"
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun delete(@PathVariable id: Long): String {
        passwordEntryService.delete(id)
        return "redirect:/admin/passwords"
    }
}