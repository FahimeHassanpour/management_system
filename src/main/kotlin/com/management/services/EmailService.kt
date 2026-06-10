package com.management.services

import com.management.models.PasswordEntry
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String,
    @Value("\${spring.mail.username:noreply@localhost}") private val fromAddress: String
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    fun sendInvitation(toEmail: String, token: String) {
        val link = "$baseUrl/register?token=$token"
        val msg = SimpleMailMessage().apply {
            from = fromAddress
            setTo(toEmail)
            subject = "You're invited to register"
            text = "Open this link to complete registration (single use):\n\n$link"
        }
        sendSafely(msg, "invitation")
    }

    fun sendPasswordAssignedEmail(to: String, title: String) {
        val message = SimpleMailMessage().apply {
            setTo(to)
            subject = "New Password Assigned"
            text = "A new password has been assigned to you: $title"
        }
        sendSafely(message, "password assigned")
    }

    fun sendPasswordUpdatedEmail(to: String, title: String) {
        val message = SimpleMailMessage().apply {
            setTo(to)
            subject = "Password Updated"
            text = "The password '$title' has been updated."
        }
        sendSafely(message, "password updated")
    }

    fun sendExpiryWarning(to: String, title: String, date: String) {
        val message = SimpleMailMessage().apply {
            setTo(to)
            subject = "Password Expired or Expiring Today"
            text = "Password '$title' is expired or expiring on $date. Please check it."
        }
        sendSafely(message, "expiry warning")
    }

    fun sendExpirySummaryEmail(
        to: String,
        entries: List<PasswordEntry>
    ) {
        val message = SimpleMailMessage()

        message.setTo(to)
        message.subject = "Expired / Expiring Passwords Summary"

        val body = StringBuilder()
        body.append("The following passwords are expired or expiring today:\n\n")

        entries.forEach { entry ->
            body.append("- ${entry.title} | ${entry.username} | ${entry.expiryDate}\n")
        }

        message.text = body.toString()

        sendSafely(message, "expiry summary")
    }

    private fun isValidEmail(email: String): Boolean =
        try {
            InternetAddress(email.trim()).validate()
            true
        } catch (_: AddressException) {
            false
        }

    private fun sendSafely(message: SimpleMailMessage, context: String) {
        val recipient = message.to?.firstOrNull()?.trim()
        if (recipient.isNullOrBlank()) {
            log.warn("Skipping email ({}): no recipient", context)
            return
        }
        if (!isValidEmail(recipient)) {
            log.warn("Skipping email ({}): invalid recipient address '{}'", context, recipient)
            return
        }
        if (message.from.isNullOrBlank()) {
            message.from = fromAddress
        }
        try {
            mailSender.send(message)
        } catch (ex: MailException) {
            log.warn("Failed to send email ({}) to '{}': {}", context, recipient, ex.message)
        }
    }
}
