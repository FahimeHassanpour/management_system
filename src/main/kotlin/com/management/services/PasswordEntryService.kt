package com.management.services

import com.management.dto.PasswordEntryRequest
import com.management.models.PasswordEntry
import com.management.repositories.AssignmentRepository
import com.management.repositories.CategoryRepository
import com.management.repositories.PasswordEntryRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

@Service
class PasswordEntryService(
    private val passwordEntryRepository: PasswordEntryRepository,
    private val categoryRepository: CategoryRepository,
    private val assignmentRepository: AssignmentRepository,
    private val emailService: EmailService
) {
    fun list(query: String?, categoryId: Long?): List<PasswordEntry> =
        passwordEntryRepository.search(query, categoryId)

    private fun resolveRequiredCategory(categoryId: Long?) =
        categoryId
            ?.let { categoryRepository.findById(it).orElseThrow { RuntimeException("Category not found: $it") } }
            ?: throw RuntimeException("Category is required")

    fun create(request: PasswordEntryRequest): PasswordEntry {
        val entry = PasswordEntry(
            title = request.title.trim(),
            username = request.username.trim(),
            password = request.password,
            description = request.description.trim(),
            category = resolveRequiredCategory(request.categoryId),
            expiryDate = parseExpiryDate(request.expiryDate)
        )
        return passwordEntryRepository.save(entry)
    }

    fun getById(id: Long): PasswordEntry {
        return passwordEntryRepository.findById(id)
            .orElseThrow { RuntimeException("Password entry not found: $id") }
    }

    fun update(id: Long, request: PasswordEntryRequest): PasswordEntry {
        val existing = getById(id)
        existing.title = request.title.trim()
        existing.username = request.username.trim()
        existing.password = request.password
        existing.description = request.description.trim()
        existing.category = resolveRequiredCategory(request.categoryId)
        existing.expiryDate = parseExpiryDate(request.expiryDate)
        val saved = passwordEntryRepository.save(existing)


        val assignments = assignmentRepository.findByPasswordEntry(saved)
        val users = assignments.mapNotNull { it.user }


        users.forEach { user ->
            emailService.sendPasswordUpdatedEmail(user.email, saved.title)
        }

        return saved
    }

    fun delete(id: Long) {
        if (!passwordEntryRepository.existsById(id)) {
            throw RuntimeException("Password entry not found: $id")
        }
        passwordEntryRepository.deleteById(id)
    }

    private fun parseExpiryDate(value: String?): LocalDateTime? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return try {
            // accepts yyyy-MM-dd from <input type="date"> and stores at start of day
            LocalDate.parse(raw).atStartOfDay()
        } catch (ex: DateTimeParseException) {
            throw RuntimeException("Invalid expiry date format: $raw")
        }
    }


}
