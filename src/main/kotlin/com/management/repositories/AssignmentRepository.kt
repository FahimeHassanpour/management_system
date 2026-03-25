package com.management.repositories

import com.management.models.Assignment
import com.management.models.User
import org.springframework.data.jpa.repository.JpaRepository

interface AssignmentRepository : JpaRepository<Assignment, Long> {

    fun findByUser(user: User): List<Assignment>
}