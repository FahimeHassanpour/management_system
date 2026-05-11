package com.management.controllers

import com.management.dto.PasswordEntryRequest
import com.management.models.PasswordEntry
import com.management.repositories.CategoryRepository
import com.management.repositories.UserRepository
import com.management.services.AssignmentService
import com.management.services.PasswordEntryService
import com.management.util.PasswordExpiry
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.format.DateTimeFormatter
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/admin/passwords")
class AdminPasswordController(
    private val passwordEntryService: PasswordEntryService,
    private val categoryRepository: CategoryRepository,
    private val assignmentService: AssignmentService,
    private val userRepository: UserRepository
) {
    private val dtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val privilegedRoles = setOf("ADMIN", "MANAGER")

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun list(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        model: Model
    ): String {
        val normalizedCategoryId = categoryId?.takeIf { it != 0L }
        val entriesPage = passwordEntryService.list(
            query,
            normalizedCategoryId,
            page,
            size
        )
        populatePasswordListModel(model, entriesPage, page, size, query, normalizedCategoryId)
        return "admin/password-list"
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun createForm(model: Model): String {
        model.addAttribute("entryRequest", PasswordEntryRequest("", "", ""))
        model.addAttribute("categories", categoryRepository.findAll())
        model.addAttribute("users", assignableUsers())
        model.addAttribute("selectedUserIds", emptySet<Long>())
        model.addAttribute("isEdit", false)
        return "admin/password-form"
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun editForm(@PathVariable id: Long, model: Model): String {
        val entry: PasswordEntry = passwordEntryService.getById(id)
        val selectedUserIds = assignmentService.assignedUserIdsForEntry(id)

        model.addAttribute(
            "entryRequest",
            PasswordEntryRequest(
                title = entry.title,
                username = entry.username,
                password = entry.password,
                description = entry.description,
                categoryId = entry.category?.id,
                userIds = selectedUserIds.toList(),
                expiryDate = entry.expiryDate?.format(dtFormatter)
            )
        )
        model.addAttribute("entryId", id)
        model.addAttribute("categories", categoryRepository.findAll())
        model.addAttribute("users", assignableUsers())
        model.addAttribute("selectedUserIds", selectedUserIds)
        model.addAttribute("isEdit", true)
        return "admin/password-form"
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun create(@ModelAttribute entryRequest: PasswordEntryRequest): String {
        val savedEntry = passwordEntryService.create(entryRequest)
        assignmentService.syncAssignments(savedEntry.id!!, entryRequest.userIds.toSet())
        return "redirect:/admin/passwords"
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun update(
        @PathVariable id: Long,
        @ModelAttribute entryRequest: PasswordEntryRequest,
        redirectAttributes: RedirectAttributes
    ): String {
        passwordEntryService.update(id, entryRequest)
        assignmentService.syncAssignments(id, entryRequest.userIds.toSet())

        redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully and users notified")

        return "redirect:/admin/passwords"
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun delete(@PathVariable id: Long): String {
        passwordEntryService.delete(id)
        return "redirect:/admin/passwords"
    }

    private fun assignableUsers() = userRepository.findAll()
        .filter { user ->
            val roleName = user.role?.name?.trim()?.uppercase()
            roleName == null || roleName !in privilegedRoles
        }

    /** Single string per entry for data-users (avoids commas inside th:attr / #strings.listJoin in HTML). */
    private fun joinedUserLabels(entryUsers: Map<Long, List<String>>): Map<Long, String> =
        entryUsers.mapValues { (_, labels) -> labels.joinToString("||") }

    @GetMapping("/fragments")
    fun listFragment(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        model: Model
    ): String {
        val normalizedCategoryId = categoryId?.takeIf { it != 0L }
        val entriesPage = passwordEntryService.list(query, normalizedCategoryId, page, size)
        populatePasswordListModel(model, entriesPage, page, size, query, normalizedCategoryId)
        return "admin/password-list :: content"
    }


    private fun populatePasswordListModel(
        model: Model,
        entriesPage: org.springframework.data.domain.Page<PasswordEntry>,
        page: Int,
        size: Int,
        query: String?,
        normalizedCategoryId: Long?
    ) {
        val entries = entriesPage.content

        val entryUsers: Map<Long, List<String>> = entries
            .mapNotNull { entry ->
                val entryId = entry.id ?: return@mapNotNull null
                val userIds = assignmentService.assignedUserIdsForEntry(entryId)
                val users = if (userIds.isEmpty()) {
                    emptyList()
                } else {
                    userRepository.findAllWithRolesByIds(userIds)
                        .filter { user ->
                            val roleName = user.role?.name?.trim()?.uppercase()
                            roleName == null || roleName !in privilegedRoles
                        }
                        .map { "${it.username} (${it.email})" }
                        .sorted()
                }
                entryId to users
            }
            .toMap()

        model.addAttribute("entries", entries)
        model.addAttribute("entryUsers", entryUsers)
        model.addAttribute("entryUsersJoined", joinedUserLabels(entryUsers))
        model.addAttribute("expiryDueIdStrings", PasswordExpiry.dueOrExpiredIdStrings(entries))
        model.addAttribute("currentPage", page)
        model.addAttribute("totalPages", entriesPage.totalPages)
        model.addAttribute("totalElements", entriesPage.totalElements)
        model.addAttribute("size", size)
        model.addAttribute("query", query ?: "")
        model.addAttribute("categories", categoryRepository.findAll())
        model.addAttribute("categoryId", normalizedCategoryId)
    }
}