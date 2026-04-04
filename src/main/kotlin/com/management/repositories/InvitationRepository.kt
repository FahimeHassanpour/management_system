package com.management.repositories

import com.management.models.Invitation
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface InvitationRepository : JpaRepository<Invitation, Long> {
    fun findByToken(token: String): Optional<Invitation>
    fun existsByEmailIgnoreCaseAndUsedAtIsNullAndExpiresAtAfter(
        email: String,
        now: java.time.Instant
    ): Boolean
}