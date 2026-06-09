package com.management.controllers

import com.management.dto.PasswordEntryRequest
import com.management.models.PasswordEntry
import com.management.repositories.CategoryRepository
import com.management.repositories.TeamRepository
import com.management.repositories.UserRepository
import com.management.services.AssignmentService
import com.management.services.DuplicatePasswordEntryException
import com.management.services.PasswordEntryService
import com.management.util.PasswordExpiry
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.time.format.DateTimeFormatter
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

@Controller
@RequestMapping("/admin/passwords")
class AdminPasswordController(
    private val passwordEntryService: PasswordEntryService,
    private val categoryRepository: CategoryRepository,
    private val assignmentService: AssignmentService,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository
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
        model.addAttribute("teams", teamRepository.findAll())

        return "admin/password-form"
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun editForm(
        @PathVariable id: Long,
        @RequestParam(required = false) success: Boolean?,
        model: Model
    ): String {
        val entry: PasswordEntry = passwordEntryService.getById(id)
        val selectedUserIds = assignmentService.assignedUserIdsForEntry(id)
        val selectedTeamIds = passwordEntryService.findTeamIdsForPassword(id)

        model.addAttribute(
            "entryRequest",
            PasswordEntryRequest(
                title = entry.title,
                username = entry.username,
                password = entry.password,
                description = entry.description,
                email = entry.email,
                mobileNumber = entry.mobileNumber,
                categoryId = entry.category?.id,
                userIds = selectedUserIds.toList(),
                expiryDate = entry.expiryDate?.format(dtFormatter),
                teamIds = selectedTeamIds.toList()
            )
        )
        model.addAttribute("entryId", id)
        model.addAttribute("categories", categoryRepository.findAll())
        model.addAttribute("users", assignableUsers())
        model.addAttribute("selectedUserIds", selectedUserIds)
        model.addAttribute("selectedTeamIds", selectedTeamIds)
        model.addAttribute("isEdit", true)
        model.addAttribute("teams", teamRepository.findAll()
        )
        if (success == true) {
            model.addAttribute(
                "successMessage",
                "Password updated successfully."
            )
        }


        return "admin/password-form"


    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun create(
        @ModelAttribute entryRequest: PasswordEntryRequest,
        model: Model
    ): String {
        return try {
            val savedEntry = passwordEntryService.create(entryRequest)
            assignmentService.syncAssignments(savedEntry.id!!, entryRequest.userIds.toSet())
            "redirect:/admin/passwords"
        } catch (ex: DuplicatePasswordEntryException) {
            populatePasswordFormModel(model, entryRequest, isEdit = false)
            model.addAttribute("errorMessage", ex.message)
            "admin/password-form"
        }
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun update(
        @PathVariable id: Long,
        @ModelAttribute entryRequest: PasswordEntryRequest,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        return try {
            passwordEntryService.update(id, entryRequest)
            assignmentService.syncAssignments(id, entryRequest.userIds.toSet())
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Password updated successfully and users notified"
            )
            "redirect:/admin/passwords/$id/edit?success=true"
        } catch (ex: DuplicatePasswordEntryException) {
            populatePasswordFormModel(
                model,
                entryRequest,
                isEdit = true,
                entryId = id,
                selectedUserIds = entryRequest.userIds.toSet(),
                selectedTeamIds = entryRequest.teamIds.toSet()
            )
            model.addAttribute("errorMessage", ex.message)
            "admin/password-form"
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun delete(@PathVariable id: Long): String {
        passwordEntryService.delete(id)
        return "redirect:/admin/passwords"
    }

    private fun populatePasswordFormModel(
        model: Model,
        entryRequest: PasswordEntryRequest,
        isEdit: Boolean,
        entryId: Long? = null,
        selectedUserIds: Set<Long>? = null,
        selectedTeamIds: Set<Long>? = null
    ) {
        model.addAttribute("entryRequest", entryRequest)
        model.addAttribute("categories", categoryRepository.findAll())
        model.addAttribute("users", assignableUsers())
        model.addAttribute("selectedUserIds", selectedUserIds ?: entryRequest.userIds.toSet())
        model.addAttribute("selectedTeamIds", selectedTeamIds ?: entryRequest.teamIds.toSet())
        model.addAttribute("isEdit", isEdit)
        model.addAttribute("teams", teamRepository.findAll())
        if (isEdit && entryId != null) {
            model.addAttribute("entryId", entryId)
        }
    }

    private fun assignableUsers() = userRepository.findAll()
        .filter { user ->
            val roleName = user.role?.name?.trim()?.uppercase()
            roleName == null || roleName !in privilegedRoles
        }

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

    @GetMapping("/template")
    fun downloadTemplate(): ResponseEntity<ByteArray> {

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Passwords")

        val header = sheet.createRow(0)

        header.createCell(0).setCellValue("Title")
        header.createCell(1).setCellValue("Username")
        header.createCell(2).setCellValue("Password")
        header.createCell(3).setCellValue("Category")
        header.createCell(4).setCellValue("Expiry Date")
        header.createCell(5).setCellValue("Email")
        header.createCell(6).setCellValue("Mobile Number")
        header.createCell(7).setCellValue("Description")
        header.createCell(8).setCellValue("user_id")
        header.createCell(9).setCellValue("team_id")


        val output = ByteArrayOutputStream()

        workbook.write(output)
        workbook.close()

        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=password-template.xlsx"
            )
            .contentType(
                MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
            .body(output.toByteArray())
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun importPasswords(
        @RequestParam("file") file: MultipartFile,
        redirectAttributes: RedirectAttributes
    ): String {
        return try {
            val imported = passwordEntryService.importExcel(file)
            redirectAttributes.addFlashAttribute(
                "successMessage",
                if (imported > 0) {
                    "Imported $imported password entr${if (imported == 1) "y" else "ies"} from Excel."
                } else {
                    "No password entries were imported. Rows may be empty or invalid."
                }
            )
            "redirect:/admin/passwords"
        } catch (ex: Exception) {
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                ex.message ?: "Failed to import Excel file."
            )
            "redirect:/admin/passwords"
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    fun exportPasswords(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) categoryId: Long?
    ): ResponseEntity<ByteArray> {
        val normalizedCategoryId = categoryId?.takeIf { it != 0L }
        val entries = passwordEntryService.listForExport(query, normalizedCategoryId)
        val bytes = passwordEntryService.exportExcel(entries)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val filename = "passwords-$timestamp.xlsx"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(
                MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
            .body(bytes)
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
                        .map { "${it.username} (${it.email})" }
                        .sorted()
                }
                val teamLabels =
                    teamRepository.findAllById(passwordEntryService.findTeamIdsForPassword(entryId))
                        .map { team -> "Team: ${team.name}" }
                        .sorted()
                entryId to (users + teamLabels).sorted()
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