package com.management.services

import com.management.models.Assignment
import com.management.repositories.AssignmentRepository
import com.management.repositories.PasswordEntryRepository
import com.management.repositories.TeamPasswordAssignmentRepository
import com.management.repositories.UserRepository
import com.management.repositories.UserTeamRepository
import org.springframework.stereotype.Service

@Service
class AssignmentService(
    private val assignmentRepository: AssignmentRepository,
    private val userRepository: UserRepository,
    private val passwordEntryRepository: PasswordEntryRepository,
    private val userTeamRepository: UserTeamRepository,
    private val teamPasswordAssignmentRepository: TeamPasswordAssignmentRepository,
    private val emailService: EmailService
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

        emailService.sendPasswordAssignedEmail(user.email, entry.title)


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

    fun assignedEntryIdsForUsernameIncludingTeams(username: String): Set<Long> {
        val user = userRepository.findByUsername(username)
            .orElseThrow { RuntimeException("User not found: $username") }

        val directIds = assignmentRepository.findByUser(user)
            .mapNotNull { it.passwordEntry?.id }
            .toSet()

        val userId = user.id ?: return directIds
        val teamIds = userTeamRepository.findTeamIdsByUserId(userId)
        if (teamIds.isEmpty()) return directIds

        val teamIdsAccess = teamPasswordAssignmentRepository.findPasswordIdsByTeamIds(teamIds)
        return directIds + teamIdsAccess
    }

    fun isAssignedToUser(passwordEntryId: Long, username: String): Boolean {
        val user = userRepository.findByUsername(username)
            .orElseThrow { RuntimeException("User not found: $username") }
        val entry = passwordEntryRepository.findById(passwordEntryId)
            .orElseThrow { RuntimeException("Password entry not found: $passwordEntryId") }
        return assignmentRepository.existsByUserAndPasswordEntry(user, entry)
    }

    fun isAssignedToUserIncludingTeams(passwordEntryId: Long, username: String): Boolean {
        return assignedEntryIdsForUsernameIncludingTeams(username).contains(passwordEntryId)
    }

    fun assignedUserIdsForEntry(passwordEntryId: Long): Set<Long> {
        val entry = passwordEntryRepository.findById(passwordEntryId)
            .orElseThrow { RuntimeException("Password entry not found: $passwordEntryId") }
        return assignmentRepository.findByPasswordEntry(entry)
            .mapNotNull { it.user?.id }
            .toSet()
    }

    /** Direct user assignments plus every member of teams that have this password. */
    fun assignedUserIdsForEntryIncludingTeams(passwordEntryId: Long): Set<Long> {
        val direct = assignedUserIdsForEntry(passwordEntryId)
        val teamIds = teamPasswordAssignmentRepository.findTeamIdsByPasswordId(passwordEntryId)
        if (teamIds.isEmpty()) return direct
        val viaTeams = userTeamRepository.findUserIdsByTeamIds(teamIds)
        return direct + viaTeams
    }

    fun syncAssignments(passwordEntryId: Long, requestedUserIds: Set<Long>) {
        val currentUserIds = assignedUserIdsForEntry(passwordEntryId)

        val idsToAssign = requestedUserIds - currentUserIds
        val idsToRemove = currentUserIds - requestedUserIds

        idsToAssign.forEach { userId -> assign(passwordEntryId, userId) }
        idsToRemove.forEach { userId -> unassign(passwordEntryId, userId) }
    }


}
