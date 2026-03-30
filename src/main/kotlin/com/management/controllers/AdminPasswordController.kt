package com.management.controllers

import com.management.dto.PasswordEntryRequest
import com.management.models.PasswordEntry
import com.management.repositories.CategoryRepository
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
@PreAuthorize("hasRole('ADMIN')")
class AdminPasswordController(
    private val passwordEntryService: PasswordEntryService,
    private val categoryRepository: CategoryRepository
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) query: String?,
        model: Model
    ): String {
        model.addAttribute("entries", passwordEntryService.list(query))
        model.addAttribute("query", query ?: "")
        return "admin/password-list"
    }

    @GetMapping("/new")
    fun createForm(model: Model): String {
        model.addAttribute("entryRequest", PasswordEntryRequest("", "", "", "", null))
        model.addAttribute("categories", categoryRepository.findAll())
        model.addAttribute("isEdit", false)
        return "admin/password-form"
    }

    @PostMapping
    fun create(@ModelAttribute entryRequest: PasswordEntryRequest): String {
        passwordEntryService.create(entryRequest)
        return "redirect:/admin/passwords"
    }

    @GetMapping("/{id}/edit")
    fun editForm(@PathVariable id: Long, model: Model): String {
        val entry: PasswordEntry = passwordEntryService.getById(id)
        model.addAttribute(
            "entryRequest",
            PasswordEntryRequest(
                title = entry.title,
                username = entry.username,
                password = entry.password,
                description = entry.description,
                categoryId = entry.category?.id
            )
        )
        model.addAttribute("entryId", id)
        model.addAttribute("categories", categoryRepository.findAll())
        model.addAttribute("isEdit", true)
        return "admin/password-form"
    }

    @PostMapping("/{id}")
    fun update(@PathVariable id: Long, @ModelAttribute entryRequest: PasswordEntryRequest): String {
        passwordEntryService.update(id, entryRequest)
        return "redirect:/admin/passwords"
    }

    @PostMapping("/{id}/delete")
    fun delete(@PathVariable id: Long): String {
        passwordEntryService.delete(id)
        return "redirect:/admin/passwords"
    }
}
