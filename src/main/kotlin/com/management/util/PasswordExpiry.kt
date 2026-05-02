package com.management.util

import com.management.models.PasswordEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.HashSet

/**
 * Calendar-day expiry: due on or before [today] (inclusive), same rule as the daily email job.
 */
object PasswordExpiry {
    fun isDueOrExpired(expiryDate: LocalDateTime?, today: LocalDate = LocalDate.now()): Boolean {
        val expiry = expiryDate?.toLocalDate() ?: return false
        return !expiry.isAfter(today)
    }

    /**
     * Always a [HashSet] (never Kotlin [emptySet]) so Thymeleaf/SpEL can call [java.util.Set.contains].
     */
    fun dueOrExpiredIdStrings(entries: Iterable<PasswordEntry>): HashSet<String> {
        val out = HashSet<String>()
        for (entry in entries) {
            val id = entry.id ?: continue
            if (isDueOrExpired(entry.expiryDate)) {
                out.add(id.toString())
            }
        }
        return out
    }
}
