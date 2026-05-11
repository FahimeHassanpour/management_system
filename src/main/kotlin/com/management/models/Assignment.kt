package com.management.models

import jakarta.persistence.*
import org.hibernate.envers.Audited
import java.time.LocalDateTime

@Entity
@Audited
@Table(name = "assignments")
data class Assignment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @ManyToOne
    @JoinColumn(name = "password_entry_id")
    var passwordEntry: PasswordEntry? = null,

    var expiresAt: LocalDateTime? = null
)