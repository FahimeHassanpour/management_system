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
}
