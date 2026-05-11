package com.management.repositories

import com.management.models.PasswordEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PasswordEntryRepository : JpaRepository<PasswordEntry, Long> {
    @Query(
        """
        SELECT p FROM PasswordEntry p
        WHERE (:query IS NULL OR :query = '' OR
               LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
        """
    )
    fun search(
        @Param("query") query: String?,
        @Param("categoryId") categoryId: Long?,
        pageable: Pageable
    ): Page<PasswordEntry>

    fun countByCategoryId(categoryId: Long): Long
}