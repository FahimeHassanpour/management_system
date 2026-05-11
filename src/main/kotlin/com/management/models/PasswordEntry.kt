package com.management.models

import jakarta.persistence.*
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.time.LocalDateTime

@Entity
@Table(name = "password_entries")
@Audited
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
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var category: Category? = null,

    @Column(name = "expiry_date")
    var expiryDate: LocalDateTime? = null,

)

