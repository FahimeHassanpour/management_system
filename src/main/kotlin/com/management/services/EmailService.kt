package com.management.services

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
}