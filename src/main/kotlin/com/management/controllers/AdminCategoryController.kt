package com.management.controllers

import com.management.models.Category
import com.management.repositories.CategoryRepository
import com.management.repositories.PasswordEntryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        model: Model
    ): String {
        val safePage = if (page < 0) 0 else page
        val safeSize = if (size < 1) 10 else size

        val sort = Sort.by(Sort.Order.asc("name").ignoreCase())
        val pageResult = categoryRepository.findAll(PageRequest.of(safePage, safeSize, sort))

        model.addAttribute("categories", pageResult.content)
        model.addAttribute("currentPage", pageResult.number)
        model.addAttribute("totalPages", pageResult.totalPages)
        model.addAttribute("totalElements", pageResult.totalElements)
        model.addAttribute("size", safeSize)
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