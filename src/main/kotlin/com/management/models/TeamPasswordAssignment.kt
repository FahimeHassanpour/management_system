package com.management.models


import jakarta.persistence.*

@Entity
@Table(name = "team_password_assignments")

class TeamPasswordAssignment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    val id: Long? = null,

    @ManyToOne
    @JoinColumn(name = "team_id")

    val team: Team,

    @ManyToOne
    @JoinColumn(name = "password_id")

    val password: PasswordEntry
)