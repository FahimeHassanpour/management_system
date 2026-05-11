package com.management.dto

import java.time.LocalDateTime

data class AuditLogEntry(
    val rev: Int,
    val performedBy: String?,
    val changedAt: LocalDateTime,
    val entityType: String,
    val entityId: Long?,
    val action: String,
    val summary: String?
)
