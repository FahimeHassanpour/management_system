package com.management.models

import jakarta.persistence.*
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionListener
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import org.springframework.security.core.context.SecurityContextHolder
import java.io.Serializable

@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditRevisionListener::class)
open class AuditRevision : Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    var id: Int = 0

    @RevisionTimestamp
    @Column(name = "revtstmp")
    var timestamp: Long = 0

    @Column(name = "performed_by")
    var performedBy: String? = null
}

class AuditRevisionListener : RevisionListener {
    override fun newRevision(revisionEntity: Any) {
        val rev = revisionEntity as AuditRevision
        // SecurityContext check to get the Admin/Manager username
        val auth = SecurityContextHolder.getContext().authentication
        rev.performedBy = auth?.name ?: "System"
    }
}