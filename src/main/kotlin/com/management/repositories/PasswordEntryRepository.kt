package com.management.repositories

import com.management.models.PasswordEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PasswordEntryRepository : JpaRepository<PasswordEntry, Long> {
    @Query(
        """
        SELECT p FROM PasswordEntry p
        WHERE (:query IS NULL OR :query = '' OR
               LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))
        """
    )
    fun search(@Param("query") query: String?): List<PasswordEntry>
}