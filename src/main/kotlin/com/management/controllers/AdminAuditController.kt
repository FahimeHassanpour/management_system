package com.management.controllers

import com.management.services.AuditLogService
import com.management.services.RetentionService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Controller
@RequestMapping("/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
class AdminAuditController(
    private val auditLogService: AuditLogService,
    private val retentionService: RetentionService
) {
    private val log = LoggerFactory.getLogger(AdminAuditController::class.java)

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val safePage = if (page < 0) 0 else page
        val safeSize = if (size < 1) 20 else size
        val pageResult = auditLogService.list(PageRequest.of(safePage, safeSize))

        model.addAttribute("entries", pageResult.content)
        model.addAttribute("currentPage", pageResult.number)
        model.addAttribute("totalPages", pageResult.totalPages)
        model.addAttribute("totalElements", pageResult.totalElements)
        model.addAttribute("size", safeSize)
        return "admin/audit-list"
    }

    @GetMapping("/export.pdf")
    fun exportPdf(): ResponseEntity<ByteArray> {
        val entries = auditLogService.listAll()
        val pdf = auditLogService.renderPdf(entries)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val filename = "audit-log-$timestamp.pdf"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf)
    }

    @PostMapping("/{rev}/delete")
    fun deleteRevision(
        @PathVariable rev: Int,
        redirectAttributes: RedirectAttributes
    ): String {
        return try {
            val deleted = auditLogService.deleteRevision(rev)
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Revision $rev deleted ($deleted master record removed)."
            )
            "redirect:/admin/audit"
        } catch (ex: Exception) {
            log.error("Delete revision $rev failed", ex)
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Failed to delete revision $rev: ${ex.message ?: ex.javaClass.simpleName}"
            )
            "redirect:/admin/audit"
        }
    }

    @PostMapping("/delete-all")
    fun deleteAll(redirectAttributes: RedirectAttributes): String {
        return try {
            val deleted = auditLogService.deleteAll()
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "All audit history deleted ($deleted revision records removed)."
            )
            "redirect:/admin/audit"
        } catch (ex: Exception) {
            log.error("Delete-all audit failed", ex)
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Failed to delete audit history: ${ex.message ?: ex.javaClass.simpleName}"
            )
            "redirect:/admin/audit"
        }
    }

    @PostMapping("/purge")
    fun purgeNow(redirectAttributes: RedirectAttributes): String {
        return try {
            val purged = retentionService.purgeOlderThanMonths(6)
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Audit retention purge complete — $purged revision record(s) removed."
            )
            "redirect:/admin/audit"
        } catch (ex: Exception) {
            log.error("Manual audit purge failed.", ex)
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Audit purge failed: ${ex.message ?: ex.javaClass.simpleName}"
            )
            "redirect:/admin/audit"
        }
    }
}
