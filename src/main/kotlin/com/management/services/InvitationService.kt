package com.management.services

import com.management.models.Invitation
import com.management.models.User
import com.management.repositories.InvitationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class InvitationService(
    private val invitationRepository: InvitationRepository
) {
    private val random = SecureRandom()

    fun generateToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    @Transactional
    fun createInvitation(email: String, admin: User, validDays: Long = 7L): Invitation {
        val normalized = email.trim().lowercase()
        val inv = Invitation(
            email = normalized,
            token = generateToken(),
            expiresAt = Instant.now().plus(validDays, ChronoUnit.DAYS),
            createdAt = Instant.now(),
            createdBy = admin
        )
        return invitationRepository.save(inv)
    }

    fun findValidByToken(token: String): Invitation? {
        val inv = invitationRepository.findByToken(token).orElse(null) ?: return null
        if (inv.usedAt != null) return null
        if (inv.expiresAt.isBefore(Instant.now())) return null
        return inv
    }

    @Transactional
    fun markUsed(invitation: Invitation) {
        invitation.usedAt = Instant.now()
        invitationRepository.save(invitation)
    }
}