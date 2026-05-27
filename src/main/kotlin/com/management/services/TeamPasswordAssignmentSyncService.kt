package com.management.services

import com.management.models.TeamPasswordAssignment
import com.management.repositories.PasswordEntryRepository
import com.management.repositories.TeamPasswordAssignmentRepository
import com.management.repositories.TeamRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Runs team↔password link updates in a single transaction. `@Modifying` deletes on the
 * repository require an active transaction; calling them from a non-transactional service
 * causes TransactionRequiredException (500 on save).
 */
@Service
class TeamPasswordAssignmentSyncService(
    private val teamPasswordAssignmentRepository: TeamPasswordAssignmentRepository,
    private val teamRepository: TeamRepository,
    private val passwordEntryRepository: PasswordEntryRepository
) {
    @Transactional
    fun replaceAssignmentsForPassword(passwordId: Long, requestedTeamIds: Set<Long>) {
        teamPasswordAssignmentRepository.deleteByPasswordId(passwordId)

        if (requestedTeamIds.isEmpty()) return

        val teams = teamRepository.findAllById(requestedTeamIds)
        val foundIds = teams.mapNotNull { it.id }.toSet()
        val missingIds = requestedTeamIds - foundIds
        require(missingIds.isEmpty()) {
            "Team(s) not found: ${missingIds.joinToString(", ")}"
        }

        val password = passwordEntryRepository
            .findById(passwordId)
            .orElseThrow { RuntimeException("Password entry not found: $passwordId") }

        val assignments = teams.map { team ->
            TeamPasswordAssignment(team = team, password = password)
        }
        teamPasswordAssignmentRepository.saveAll(assignments)
    }
}
