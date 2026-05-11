package com.management.services

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class RetentionService(private val jdbcTemplate: JdbcTemplate) {

    private val log = LoggerFactory.getLogger(RetentionService::class.java)

    private val retentionMonths: Long = 6

    @Scheduled(cron = "0 0 3 1 * *")
    fun scheduledRetentionRun() {
        try {
            val purged = purgeOlderThanMonths(retentionMonths)
            log.info("Scheduled retention completed: {} revinfo records purged.", purged)
        } catch (ex: Exception) {
            log.error("Scheduled retention run failed.", ex)
        }
    }

    @Transactional
    fun purgeOlderThanMonths(months: Long): Int {
        log.info("Starting retention purge for revisions older than {} months.", months)

        val cutoffDate = LocalDateTime.now().minusMonths(months)
        val cutoffTimestamp = cutoffDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val maxRevToDelete: Long? = jdbcTemplate.queryForObject(
            "SELECT MAX(rev) FROM revinfo WHERE revtstmp < ?",
            Long::class.java,
            cutoffTimestamp
        )

        if (maxRevToDelete == null || maxRevToDelete <= 0) {
            log.info("No revinfo entries older than {} (cutoff). Nothing to purge.", cutoffDate)
            return 0
        }

        log.info("Purging revisions <= {} (older than {}).", maxRevToDelete, cutoffDate)

        val auditTables = findAuditTableNames()
        log.info("Discovered {} audit tables: {}", auditTables.size, auditTables)

        var totalChildRows = 0
        for (table in auditTables) {
            val deleted = jdbcTemplate.update(
                "DELETE FROM $table WHERE rev <= ?",
                maxRevToDelete
            )
            totalChildRows += deleted
            log.info("  - {}: {} row(s) purged", table, deleted)
        }

        val deletedRevinfo = jdbcTemplate.update(
            "DELETE FROM revinfo WHERE rev <= ?",
            maxRevToDelete
        )

        log.info(
            "Retention purge done: {} revinfo records purged, {} child audit rows purged.",
            deletedRevinfo,
            totalChildRows
        )
        return deletedRevinfo
    }


    private fun findAuditTableNames(): List<String> {
        val raw: List<String?> = jdbcTemplate.queryForList(
            """
            SELECT table_name
              FROM information_schema.tables
             WHERE table_schema = current_schema()
               AND table_name LIKE '%\_aud' ESCAPE '\'
            """.trimIndent(),
            String::class.java
        )
        return raw.filterNotNull()
    }
}
