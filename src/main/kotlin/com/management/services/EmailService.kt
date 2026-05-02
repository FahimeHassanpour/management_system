package com.management.services

import com.management.models.PasswordEntry
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String,
    @Value("\${spring.mail.username:noreply@localhost}") private val fromAddress: String
) {
    fun sendInvitation(toEmail: String, token: String) {
        val link = "$baseUrl/register?token=$token"
        val msg = SimpleMailMessage().apply {
            from = fromAddress
            setTo(toEmail)
            subject = "You're invited to register"
            text = "Open this link to complete registration (single use):\n\n$link"
        }
        mailSender.send(msg)
    }


    fun sendPasswordAssignedEmail(to: String, title: String) {
        val message = SimpleMailMessage()
        message.setTo(to)
        message.subject = "New Password Assigned"
        message.text = "A new password has been assigned to you: $title"

        mailSender.send(message)
    }

    fun sendPasswordUpdatedEmail(to: String, title: String) {
        val message = SimpleMailMessage()
        message.setTo(to)
        message.subject = "Password Updated"
        message.text = "The password '$title' has been updated."

        mailSender.send(message)
    }

    fun sendExpiryWarning(to: String, title: String, date: String) {
        val message = SimpleMailMessage()
        message.setTo(to)
        message.subject = "Password Expired or Expiring Today"
        message.text = "Password '$title' is expired or expiring on $date. Please check it."

        mailSender.send(message)
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

        mailSender.send(message)
    }

}