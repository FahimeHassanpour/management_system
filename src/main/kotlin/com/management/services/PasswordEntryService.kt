package com.management.services

import com.management.dto.PasswordEntryRequest
import com.management.models.PasswordEntry
import com.management.repositories.CategoryRepository
import com.management.repositories.PasswordEntryRepository
import org.springframework.stereotype.Service

@Service
class PasswordEntryService(
    private val passwordEntryRepository: PasswordEntryRepository,
    private val categoryRepository: CategoryRepository
) {
    fun list(query: String?): List<PasswordEntry> = passwordEntryRepository.search(query)

    fun getById(id: Long): PasswordEntry {
        return passwordEntryRepository.findById(id)
            .orElseThrow { RuntimeException("Password entry not found: $id") }
    }

    fun create(request: PasswordEntryRequest): PasswordEntry {
        val category = request.categoryId?.let { categoryRepository.findById(it).orElse(null) }
        val entry = PasswordEntry(
            title = request.title.trim(),
            username = request.username.trim(),
            password = request.password,
            description = request.description.trim(),
            category = category
        )
        return passwordEntryRepository.save(entry)
    }

    fun update(id: Long, request: PasswordEntryRequest): PasswordEntry {
        val existing = getById(id)
        existing.title = request.title.trim()
        existing.username = request.username.trim()
        existing.password = request.password
        existing.description = request.description.trim()
        existing.category = request.categoryId?.let { categoryRepository.findById(it).orElse(null) }
        return passwordEntryRepository.save(existing)
    }

    fun delete(id: Long) {
        if (!passwordEntryRepository.existsById(id)) {
            throw RuntimeException("Password entry not found: $id")
        }
        passwordEntryRepository.deleteById(id)
    }
}
