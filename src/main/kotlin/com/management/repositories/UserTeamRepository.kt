package com.management.repositories

import com.management.models.UserTeam
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserTeamRepository : JpaRepository<UserTeam, Long> {

    @Modifying
    @Query("DELETE FROM UserTeam ut WHERE ut.user.id = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)

    @Query("SELECT ut.team.id FROM UserTeam ut WHERE ut.user.id = :userId")
    fun findTeamIdsByUserId(@Param("userId") userId: Long): Set<Long>

    @Query("SELECT DISTINCT ut.user.id FROM UserTeam ut WHERE ut.team.id IN :teamIds")
    fun findUserIdsByTeamIds(@Param("teamIds") teamIds: Collection<Long>): Set<Long>
}
