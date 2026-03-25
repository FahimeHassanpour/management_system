package com.management.models


import jakarta.persistence.*

@Entity
@Table(name = "password_entries")
data class PasswordEntry(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false)
    var username: String = "",

    @Column(nullable = false)
    var password: String = "",

    var description: String = "",

    @ManyToOne
    @JoinColumn(name = "category_id")
    var category: Category? = null
)