package com.management.config

import com.management.models.AuditRevision
import org.hibernate.envers.RevisionListener
import org.springframework.security.core.context.SecurityContextHolder

class AuditRevisionListener : RevisionListener {
    override fun newRevision(revisionEntity: Any) {
        val revision = revisionEntity as AuditRevision
        val auth = SecurityContextHolder.getContext().authentication
        revision.performedBy = auth?.name ?: "System"
    }
}