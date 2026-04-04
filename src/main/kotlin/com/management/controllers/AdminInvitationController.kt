package com.management.controllers

import com.management.services.EmailService
import com.management.services.InvitationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import com.management.repositories.UserRepository

@Controller
@RequestMapping("/admin/invitations")
@PreAuthorize("hasRole('ADMIN')")
class AdminInvitationController(
    private val invitationService: InvitationService,
    private val emailService: EmailService,
    private val userRepository: UserRepository
) {
    @GetMapping
    fun form(model: Model): String {
        return "admin/invite"
    }

    @PostMapping
    fun send(
        @RequestParam email: String,
        @AuthenticationPrincipal principal: UserDetails,
        model: Model
    ): String {
        val admin = userRepository.findByUsername(principal.username)
            .orElseThrow { IllegalStateException("Admin user not found") }
        val inv = invitationService.createInvitation(email, admin)
        emailService.sendInvitation(inv.email, inv.token)
        return "redirect:/admin/invitations?sent=1"
    }
}