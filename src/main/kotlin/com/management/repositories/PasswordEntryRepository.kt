package com.management.repositories

import com.management.models.PasswordEntry
import org.springframework.data.jpa.repository.JpaRepository

interface PasswordEntryRepository : JpaRepository<PasswordEntry, Long>