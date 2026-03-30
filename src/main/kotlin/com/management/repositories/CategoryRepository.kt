package com.management.repositories

import com.management.models.Category
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByName(name: String): Optional<Category>
}