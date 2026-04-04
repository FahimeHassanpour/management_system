package com.management.models

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "invitations")
class Invitation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 150)
    var email: String = "",

    @Column(nullable = false, unique = true, length = 255)
    var token: String = "",

    @Column(nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column
    var usedAt: Instant? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    var createdBy: User? = null
)