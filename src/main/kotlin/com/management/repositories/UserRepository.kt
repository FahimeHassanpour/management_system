package com.management.repositories

import com.management.models.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun findByUsername(username: String): Optional<User>

    @EntityGraph(attributePaths = ["role"])
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    fun findAllWithRolesByIds(@Param("ids") ids: Collection<Long>): List<User>
}