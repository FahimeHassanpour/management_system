package com.management.models

import jakarta.persistence.*
import org.hibernate.envers.Audited

@Entity
@Audited
@Table(name = "roles")
data class Role(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var name: String = ""
)