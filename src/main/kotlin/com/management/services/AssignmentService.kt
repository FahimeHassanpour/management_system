package com.management.services

import com.management.models.Assignment
import com.management.repositories.AssignmentRepository
import com.management.repositories.PasswordEntryRepository
import com.management.repositories.UserRepository
import org.springframework.stereotype.Service

@Service
class AssignmentService(
    private val assignmentRepository: AssignmentRepository,
    private val userRepository: UserRepository,
    private val passwordEntryRepository: PasswordEntryRepository
) {
    fun assign(passwordEntryId: Long, userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found: $userId") }
        val entry = passwordEntryRepository.findById(passwordEntryId)
            .orElseThrow { RuntimeException("Password entry not found: $passwordEntryId") }

        if (assignmentRepository.existsByUserAndPasswordEntry(user, entry)) {
            return
        }
        assignmentRepository.save(Assignment(user = user, passwordEntry = entry))
    }

    fun unassign(passwordEntryId: Long, userId: Long) {
        val entry = passwordEntryRepository.findById(passwordEntryId)
            .orElseThrow { RuntimeException("Password entry not found: $passwordEntryId") }
        val assignments = assignmentRepository.findByPasswordEntry(entry)
        val target = assignments.firstOrNull { it.user?.id == userId } ?: return
        assignmentRepository.deleteById(target.id!!)
    }

    fun assignedEntryIdsForUsername(username: String): Set<Long> {
        val user = userRepository.findByUsername(username)
            .orElseThrow { RuntimeException("User not found: $username") }
        return assignmentRepository.findByUser(user)
            .mapNotNull { it.passwordEntry?.id }
            .toSet()
    }

    fun isAssignedToUser(passwordEntryId: Long, username: String): Boolean {
        val user = userRepository.findByUsername(username)
            .orElseThrow { RuntimeException("User not found: $username") }
        val entry = passwordEntryRepository.findById(passwordEntryId)
            .orElseThrow { RuntimeException("Password entry not found: $passwordEntryId") }
        return assignmentRepository.existsByUserAndPasswordEntry(user, entry)
    }

    fun assignedUserIdsForEntry(passwordEntryId: Long): Set<Long> {
        val entry = passwordEntryRepository.findById(passwordEntryId)
            .orElseThrow { RuntimeException("Password entry not found: $passwordEntryId") }
        return assignmentRepository.findByPasswordEntry(entry)
            .mapNotNull { it.user?.id }
            .toSet()
    }
}
