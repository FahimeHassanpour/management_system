package com.management.repositories


import com.management.models.TeamPasswordAssignment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TeamPasswordAssignmentRepository :
    JpaRepository<TeamPasswordAssignment, Long> {

    @Query("SELECT tpa.team.id FROM TeamPasswordAssignment tpa WHERE tpa.password.id = :passwordId")
    fun findTeamIdsByPasswordId(@Param("passwordId") passwordId: Long): Set<Long>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TeamPasswordAssignment tpa WHERE tpa.password.id = :passwordId")
    fun deleteByPasswordId(@Param("passwordId") passwordId: Long)

    @Query("SELECT DISTINCT tpa.password.id FROM TeamPasswordAssignment tpa WHERE tpa.team.id IN :teamIds")
    fun findPasswordIdsByTeamIds(@Param("teamIds") teamIds: Collection<Long>): Set<Long>
}