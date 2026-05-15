package com.management.services

import com.management.models.UserTeam
import com.management.repositories.TeamRepository
import com.management.repositories.UserRepository
import com.management.repositories.UserTeamRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserTeamService(
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val userTeamRepository: UserTeamRepository
) {

    @Transactional
    fun assignTeams(userId: Long, teamIds: Set<Long>) {

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        // delete old assignments
        userTeamRepository.deleteByUserId(userId)

        val teams = teamRepository.findAllById(teamIds)

        val assignments = teams.map { team ->
            UserTeam(
                user = user,
                team = team
            )
        }

        userTeamRepository.saveAll(assignments)
    }
}