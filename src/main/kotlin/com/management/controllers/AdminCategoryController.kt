package com.management.controllers

import com.management.models.Category
import com.management.repositories.CategoryRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
class AdminCategoryController(
    private val categoryRepository: CategoryRepository
) {

    @GetMapping
    fun list(model: Model): String {
        model.addAttribute("categories", categoryRepository.findAll().sortedBy { it.name.lowercase() })
        return "admin/category-list"
    }

    @PostMapping
    fun create(
        @RequestParam name: String,
        model: Model
    ): String {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            model.addAttribute("error", "Category name is required.")
            model.addAttribute("categories", categoryRepository.findAll().sortedBy { it.name.lowercase() })
            return "admin/category-list"
        }

        if (categoryRepository.findByName(trimmedName).isPresent) {
            model.addAttribute("error", "A category with this name already exists.")
            model.addAttribute("categories", categoryRepository.findAll().sortedBy { it.name.lowercase() })
            return "admin/category-list"
        }

        categoryRepository.save(
            Category(
                name = trimmedName,
            )
        )
        return "redirect:/admin/categories"
    }
}