package com.management.controllers

import com.management.models.Category
import com.management.repositories.CategoryRepository
import com.management.repositories.PasswordEntryRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
class AdminCategoryController(
    private val categoryRepository: CategoryRepository,
    private val passwordEntryRepository: PasswordEntryRepository
) {

    @GetMapping
    fun list(model: Model): String {
        model.addAttribute(
            "categories",
            categoryRepository.findAll().sortedBy { it.name.lowercase() }
        )
        return "admin/category-list"
    }

    @PostMapping
    fun create(@RequestParam name: String): String {
        val trimmed = name.trim()

        if (trimmed.isNotEmpty()) {
            if (categoryRepository.findByName(trimmed).isEmpty) {
                categoryRepository.save(Category(name = trimmed))
            }
        }

        return "redirect:/admin/categories"
    }

    @PostMapping("/update")
    fun update(@RequestParam id: Long, @RequestParam name: String): String {
        val category = categoryRepository.findById(id).orElseThrow()
        category.name = name
        categoryRepository.save(category)

        return "redirect:/admin/categories"

        
    }

    @PostMapping("/delete")
    @ResponseBody
    fun delete(@RequestParam id: Long): ResponseEntity<Map<String, String>> {
        val usageCount = passwordEntryRepository.countByCategoryId(id)

        if (usageCount > 0) {
            return ResponseEntity.badRequest().body(
                mapOf("message" to "Cannot delete this category because it is used by existing password entries.")
            )
        }

        categoryRepository.deleteById(id)
        return ResponseEntity.ok(mapOf("message" to "Category deleted."))
    }


    @GetMapping("/admin/categories")
    fun categories(): String {
        return "categories :: content"
    }


}