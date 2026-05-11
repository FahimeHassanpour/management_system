package com.management.models


import jakarta.persistence.*
import org.hibernate.envers.Audited

@Entity
@Audited
@Table(name = "categories")
data class Category(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    var description: String = ""
)

