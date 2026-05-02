package com.management.services

import com.management.repositories.PasswordEntryRepository
import com.management.repositories.UserRepository
import com.management.util.PasswordExpiry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PasswordExpiryScheduler(
    private val passwordEntryRepository: PasswordEntryRepository,
    private val emailService: EmailService,
    private val userRepository: UserRepository
) {

    @Scheduled(cron = "0 0 9 * * *")
    fun checkExpiredPasswords() {

        val today = LocalDate.now()

        val expiringEntries = passwordEntryRepository.findAll().filter {
            PasswordExpiry.isDueOrExpired(it.expiryDate, today)
        }

        if (expiringEntries.isEmpty()) return

        val adminsManagers = userRepository.findAll().filter {
            it.role?.name in listOf("ADMIN", "MANAGER")
        }

        adminsManagers.forEach { user ->
            emailService.sendExpirySummaryEmail(
                user.email,
                expiringEntries
            )
        }
    }

}