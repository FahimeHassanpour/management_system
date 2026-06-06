package com.management.services

import com.management.dto.PasswordEntryRequest
import com.management.models.Category
import com.management.models.PasswordEntry
import com.management.repositories.AssignmentRepository
import com.management.repositories.CategoryRepository
import com.management.repositories.PasswordEntryRepository
import com.management.repositories.TeamPasswordAssignmentRepository
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


@Service
class PasswordEntryService(

    private val passwordEntryRepository:
    PasswordEntryRepository,

    private val categoryRepository:
    CategoryRepository,

    private val assignmentRepository:
    AssignmentRepository,

    private val emailService:
    EmailService,
    private val teamPasswordAssignmentRepository:
    TeamPasswordAssignmentRepository,

    private val teamPasswordAssignmentSyncService: TeamPasswordAssignmentSyncService
) {

    private val cellFormatter =
        DataFormatter()

    private fun nullableTrim(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }

    fun list(
        query: String?,
        categoryId: Long?,
        page: Int,
        size: Int

    ): Page<PasswordEntry> {

        return passwordEntryRepository.search(
            query,
            categoryId,
            PageRequest.of(page, size)
        )
    }

    private fun resolveRequiredCategory(
        categoryId: Long?
    ): Category {

        return categoryId
            ?.let {
                categoryRepository
                    .findById(it)
                    .orElseThrow {
                        RuntimeException(
                            "Category not found: $it"
                        )
                    }
            }

            ?: throw RuntimeException(
                "Category is required"
            )
    }

    fun create(
        request: PasswordEntryRequest
    ): PasswordEntry {

        val entry =
            PasswordEntry(

                title =
                    request.title.trim(),

                username =
                    request.username.trim(),

                password =
                    request.password,

                email =
                    nullableTrim(request.email),

                mobileNumber =
                    nullableTrim(request.mobileNumber),

                description =
                    request.description.trim(),

                category =
                    resolveRequiredCategory(
                        request.categoryId
                    ),

                expiryDate =
                    parseExpiryDate(
                        request.expiryDate
                    )
            )


        val savedPassword =
            passwordEntryRepository
                .save(entry)


        teamPasswordAssignmentSyncService.replaceAssignmentsForPassword(
            savedPassword.id!!,
            request.teamIds.toSet()
        )

        return savedPassword


    }

    fun getById(
        id: Long
    ): PasswordEntry {

        return passwordEntryRepository
            .findById(id)

            .orElseThrow {

                RuntimeException(
                    "Password entry not found: $id"
                )
            }
    }

    fun update(
        id: Long,
        request: PasswordEntryRequest

    ): PasswordEntry {

        val existing =
            getById(id)

        existing.title =
            request.title.trim()

        existing.username =
            request.username.trim()

        existing.password =
            request.password

        existing.email =
            nullableTrim(request.email)

        existing.mobileNumber =
            nullableTrim(request.mobileNumber)

        existing.description =
            request.description.trim()

        existing.category =
            resolveRequiredCategory(
                request.categoryId
            )

        existing.expiryDate =
            parseExpiryDate(
                request.expiryDate
            )

        val saved =
            passwordEntryRepository
                .save(existing)

        val assignments =
            assignmentRepository
                .findByPasswordEntry(saved)

        val users =
            assignments
                .mapNotNull {
                    it.user
                }

        users.forEach { user ->

            emailService
                .sendPasswordUpdatedEmail(
                    user.email,
                    saved.title
                )
        }

        teamPasswordAssignmentSyncService.replaceAssignmentsForPassword(
            saved.id!!,
            request.teamIds.toSet()
        )
        return saved
    }

    @Transactional
    fun delete(
        id: Long
    ) {

        if (
            !passwordEntryRepository
                .existsById(id)
        ) {

            throw RuntimeException(
                "Password entry not found: $id"
            )
        }

        teamPasswordAssignmentSyncService.replaceAssignmentsForPassword(id, emptySet())

        passwordEntryRepository
            .deleteById(id)
    }

    fun findTeamIdsForPassword(passwordId: Long): Set<Long> {
        return teamPasswordAssignmentRepository.findTeamIdsByPasswordId(passwordId)
    }

    fun importExcel(
        file: MultipartFile

    ): Int {

        require(
            !file.isEmpty
        ) {
            "Please choose an Excel file."
        }

        val workbook =
            WorkbookFactory
                .create(
                    file.inputStream
                )

        try {

            val sheet =
                workbook
                    .getSheetAt(0)

            var imported = 0

            for (
            i in 1..
                    sheet.lastRowNum
            ) {

                val row =
                    sheet.getRow(i)
                        ?: continue

                val title =
                    cellValue(
                        row.getCell(0)
                    )

                val username =
                    cellValue(
                        row.getCell(1)
                    )

                val password =
                    cellValue(
                        row.getCell(2)
                    )

                if (
                    title.isBlank() &&
                    username.isBlank() &&
                    password.isBlank()
                ) {

                    continue
                }

                val exists =
                    passwordEntryRepository
                        .existsByTitleAndUsername(
                            title.trim(),
                            username.trim()
                        )

                if (exists) {

                    continue
                }

                val categoryName =
                    cellValue(
                        row.getCell(3)
                    )

                val category =
                    categoryName
                        .takeIf {
                            it.isNotBlank()
                        }

                        ?.let { name ->

                            categoryRepository
                                .findByName(
                                    name.trim()
                                )

                                .orElseGet {

                                    categoryRepository
                                        .save(

                                            Category(
                                                name =
                                                    name.trim()
                                            )
                                        )
                                }
                        }

                val expiryDate =
                    cellValue(
                        row.getCell(4)
                    )

                        .takeIf {
                            it.isNotBlank()
                        }

                        ?.let {
                            parseExpiryDate(it)
                        }

                val email =
                    nullableTrim(cellValue(row.getCell(5)))

                val mobileNumber =
                    nullableTrim(cellValue(row.getCell(6)))

                passwordEntryRepository
                    .save(

                        PasswordEntry(

                            title =
                                title.trim(),

                            username =
                                username.trim(),

                            password =
                                password,

                            email =
                                email,

                            mobileNumber =
                                mobileNumber,

                            category =
                                category,

                            expiryDate =
                                expiryDate
                        )
                    )

                imported++
            }

            return imported

        } finally {

            workbook.close()
        }
    }

    private fun parseExpiryDate(
        value: String?

    ): LocalDateTime? {

        val raw =
            value
                ?.trim()
                .orEmpty()

        if (
            raw.isEmpty()
        ) {
            return null
        }

        return try {

            LocalDate
                .parse(raw)

                .atStartOfDay()

        } catch (
            ex:
            DateTimeParseException
        ) {

            throw RuntimeException(
                "Invalid expiry date: $raw"
            )
        }
    }

    private fun cellValue(
        cell:
        org.apache.poi.ss.usermodel.Cell?

    ): String {

        if (
            cell == null
        ) {
            return ""
        }

        return cellFormatter
            .formatCellValue(cell)
            .trim()
    }

    fun listForExport(
        query: String?,
        categoryId: Long?

    ): List<PasswordEntry> {

        return passwordEntryRepository
            .search(

                query,

                categoryId,

                PageRequest.of(
                    0,
                    Int.MAX_VALUE
                )

            ).content
    }

    fun exportExcel(
        entries:
        List<PasswordEntry>

    ): ByteArray {

        val workbook =
            XSSFWorkbook()

        val sheet =
            workbook
                .createSheet(
                    "Passwords"
                )

        val headers =
            listOf(
                "Title",
                "Username",
                "Password",
                "Category",
                "Expiry",
                "Email",
                "Mobile Number"
            )

        val headerRow =
            sheet.createRow(0)

        headers.forEachIndexed {
                index,
                title ->

            headerRow
                .createCell(index)
                .setCellValue(title)
        }

        val formatter =
            DateTimeFormatter
                .ofPattern(
                    "yyyy-MM-dd"
                )

        entries.forEachIndexed {
                index,
                entry ->

            val row =
                sheet.createRow(
                    index + 1
                )

            row.createCell(0)
                .setCellValue(
                    entry.title
                )

            row.createCell(1)
                .setCellValue(
                    entry.username
                )

            row.createCell(2)
                .setCellValue(
                    entry.password
                )

            row.createCell(3)
                .setCellValue(
                    entry.category?.name
                        ?: ""
                )

            row.createCell(4)
                .setCellValue(

                    entry.expiryDate
                        ?.toLocalDate()

                        ?.format(
                            formatter
                        )

                        ?: ""
                )

            row.createCell(5)
                .setCellValue(entry.email ?: "")

            row.createCell(6)
                .setCellValue(entry.mobileNumber ?: "")
        }

        return ByteArrayOutputStream()
            .use { output ->

                workbook.write(
                    output
                )

                workbook.close()

                output
                    .toByteArray()
            }
    }


}