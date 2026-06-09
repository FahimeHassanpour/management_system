package com.management.services

import com.management.dto.AuditLogEntry
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Service
class AuditLogService(private val jdbcTemplate: JdbcTemplate) {

    private val log = LoggerFactory.getLogger(AuditLogService::class.java)

    private data class TableConfig(
        val tableName: String,
        val entityType: String,
        val summarySql: String
    )

    private val tableConfigs: List<TableConfig> = listOf(
        TableConfig(
            "password_entries_aud", "PasswordEntry",
            "COALESCE(title, '') || ' / ' || COALESCE(username, '')"
        ),
        TableConfig(
            "users_aud", "User",
            "COALESCE(username, '') || ' (' || COALESCE(email, '') || ')'"
        ),
        TableConfig("categories_aud", "Category", "COALESCE(name, '')"),
        TableConfig("roles_aud", "Role", "COALESCE(name, '')"),
        TableConfig(
            "assignments_aud", "Assignment",
            "'user_id=' || COALESCE(user_id::text, '?') || ', password_id=' || COALESCE(password_entry_id::text, '?')"
        ),
        TableConfig(
            "invitations_aud", "Invitation",
            "COALESCE(email, '')"
        ),
    )

    fun list(pageable: Pageable): Page<AuditLogEntry> {
        val available = availableConfigs()
        if (available.isEmpty()) {
            return PageImpl(emptyList(), pageable, 0)
        }

        val unionSql = buildUnionSql(available)

        val total: Long = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM ($unionSql) AS u",
            Long::class.java
        ) ?: 0L

        if (total == 0L) {
            return PageImpl(emptyList(), pageable, 0)
        }

        val sql = """
            $unionSql
            ORDER BY rev DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val rows = jdbcTemplate.query(sql, { rs, _ ->
            AuditLogEntry(
                rev = rs.getInt("rev"),
                performedBy = rs.getString("performed_by"),
                changedAt = millisToLocalDateTime(rs.getLong("revtstmp")),
                entityType = rs.getString("entity_type"),
                entityId = (rs.getObject("entity_id") as? Number)?.toLong(),
                action = revTypeToAction(rs.getInt("revtype")),
                summary = rs.getString("summary")
            )
        }, pageable.pageSize, pageable.offset)

        return PageImpl(rows, pageable, total)
    }

    fun listAll(): List<AuditLogEntry> {
        val available = availableConfigs()
        if (available.isEmpty()) return emptyList()
        val sql = "${buildUnionSql(available)} ORDER BY rev DESC"
        return jdbcTemplate.query(sql) { rs, _ ->
            AuditLogEntry(
                rev = rs.getInt("rev"),
                performedBy = rs.getString("performed_by"),
                changedAt = millisToLocalDateTime(rs.getLong("revtstmp")),
                entityType = rs.getString("entity_type"),
                entityId = (rs.getObject("entity_id") as? Number)?.toLong(),
                action = revTypeToAction(rs.getInt("revtype")),
                summary = rs.getString("summary")
            )
        }
    }

    @Transactional
    fun deleteRevision(rev: Int): Int {
        log.info("Deleting audit revision rev={}", rev)
        for (table in findAuditTableNames()) {
            jdbcTemplate.update("DELETE FROM $table WHERE rev = ?", rev)
        }
        return jdbcTemplate.update("DELETE FROM revinfo WHERE rev = ?", rev)
    }

    @Transactional
    fun deleteAll(): Int {
        log.info("Deleting ALL audit revisions on admin request.")
        for (table in findAuditTableNames()) {
            jdbcTemplate.update("DELETE FROM $table")
        }
        return jdbcTemplate.update("DELETE FROM revinfo")
    }

    fun renderExcel(entries: List<AuditLogEntry>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Audit Log")
        val headers = listOf("Rev", "When", "Who", "Entity", "Action", "ID", "Summary")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            headerRow.createCell(index).setCellValue(title)
        }

        val rowFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        entries.forEachIndexed { index, entry ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(entry.rev.toDouble())
            row.createCell(1).setCellValue(entry.changedAt.format(rowFormatter))
            row.createCell(2).setCellValue(entry.performedBy ?: "-")
            row.createCell(3).setCellValue(entry.entityType)
            row.createCell(4).setCellValue(entry.action)
            row.createCell(5).setCellValue(entry.entityId?.toString() ?: "-")
            row.createCell(6).setCellValue(entry.summary ?: "-")
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        return ByteArrayOutputStream().use { output ->
            workbook.write(output)
            workbook.close()
            output.toByteArray()
        }
    }

    private fun availableConfigs(): List<TableConfig> {
        val present = findAuditTableNames().toSet()
        return tableConfigs.filter { it.tableName in present }
    }

    private fun buildUnionSql(configs: List<TableConfig>): String =
        configs.joinToString("\nUNION ALL\n") { cfg ->
            """
            SELECT r.rev,
                   r.performed_by,
                   r.revtstmp,
                   '${cfg.entityType}' AS entity_type,
                   t.id AS entity_id,
                   t.revtype,
                   (${cfg.summarySql}) AS summary
              FROM ${cfg.tableName} t
              JOIN revinfo r ON r.rev = t.rev
            """.trimIndent()
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

    private fun millisToLocalDateTime(millis: Long): LocalDateTime =
        Timestamp(millis).toLocalDateTime() ?: LocalDateTime.now()

    private fun revTypeToAction(revType: Int): String = when (revType) {
        0 -> "CREATED"
        1 -> "UPDATED"
        2 -> "DELETED"
        else -> "REV-$revType"
    }
}
