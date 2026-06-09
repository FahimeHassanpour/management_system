package com.management.services

import com.management.dto.PasswordEntryRequest
import com.management.models.Category
import com.management.models.PasswordEntry
import com.management.repositories.AssignmentRepository
import com.management.repositories.CategoryRepository
import com.management.repositories.PasswordEntryRepository
import com.management.repositories.TeamPasswordAssignmentRepository
import com.management.repositories.TeamRepository
import com.management.repositories.UserRepository
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
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

    private val teamPasswordAssignmentSyncService: TeamPasswordAssignmentSyncService,

    private val assignmentService: AssignmentService,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository
) {

    private val cellFormatter =
        DataFormatter()

    private fun nullableTrim(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }

    private fun assertUniqueTitleAndUsername(
        title: String,
        username: String,
        excludeId: Long? = null
    ) {
        val duplicate = passwordEntryRepository.findByTitleAndUsername(title, username)
        if (duplicate != null && duplicate.id != excludeId) {
            throw DuplicatePasswordEntryException()
        }
    }

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

        val title = request.title.trim()
        val username = request.username.trim()
        assertUniqueTitleAndUsername(title, username)

        val entry =
            PasswordEntry(

                title = title,

                username = username,

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

        val title = request.title.trim()
        val username = request.username.trim()
        assertUniqueTitleAndUsername(title, username, excludeId = id)

        existing.title = title

        existing.username = username

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

    @Transactional
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

            val columns = resolveImportColumns(sheet.getRow(0))
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
                        row.getCell(columns.title)
                    )

                val username =
                    cellValue(
                        row.getCell(columns.username)
                    )

                val password =
                    cellValue(
                        row.getCell(columns.password)
                    )

                if (
                    title.isBlank() &&
                    username.isBlank() &&
                    password.isBlank()
                ) {
                    continue
                }

                if (title.isBlank() || username.isBlank() || password.isBlank()) {
                    throw RuntimeException("Row ${i + 1}: Title, Username, and Password are required.")
                }

                val trimmedTitle = title.trim()
                val trimmedUsername = username.trim()
                val existingEntry =
                    passwordEntryRepository.findByTitleAndUsername(
                        trimmedTitle,
                        trimmedUsername
                    )

                if (existingEntry != null) {
                    val passwordId = existingEntry.id
                        ?: throw RuntimeException("Row ${i + 1}: Existing password entry has no id.")
                    applyImportAssignments(
                        rowIndex = i,
                        passwordId = passwordId,
                        userIdsCell = columns.userIds?.let { col -> row.getCell(col) },
                        teamIdsCell = columns.teamIds?.let { col -> row.getCell(col) },
                        syncUsers = columns.userIds != null,
                        syncTeams = columns.teamIds != null
                    )
                    imported++
                    continue
                }

                val categoryName =
                    cellValue(
                        row.getCell(columns.category)
                    )

                if (categoryName.isBlank()) {
                    throw RuntimeException("Row ${i + 1}: Category is required.")
                }

                val category =
                    categoryRepository
                        .findByName(categoryName.trim())
                        .orElseGet {
                            categoryRepository.save(Category(name = categoryName.trim()))
                        }

                val expiryDate =
                    cellValue(
                        row.getCell(columns.expiryDate)
                    )

                        .takeIf {
                            it.isNotBlank()
                        }

                        ?.let {
                            parseExpiryDate(it)
                        }

                val email =
                    nullableTrim(cellValue(row.getCell(columns.email)))

                val mobileNumber =
                    nullableTrim(cellValue(row.getCell(columns.mobileNumber)))

                val description =
                    cellValue(row.getCell(columns.description))

                val savedPassword = passwordEntryRepository.save(
                    PasswordEntry(
                        title = trimmedTitle,
                        username = trimmedUsername,
                        password = password,
                        email = email,
                        mobileNumber = mobileNumber,
                        description = description.trim(),
                        category = category,
                        expiryDate = expiryDate
                    )
                )

                val passwordId = savedPassword.id
                    ?: throw RuntimeException("Row ${i + 1}: Failed to save password entry.")

                applyImportAssignments(
                    rowIndex = i,
                    passwordId = passwordId,
                    userIdsCell = columns.userIds?.let { col -> row.getCell(col) },
                    teamIdsCell = columns.teamIds?.let { col -> row.getCell(col) },
                    syncUsers = columns.userIds != null,
                    syncTeams = columns.teamIds != null
                )

                imported++
            }

            return imported

        } finally {

            workbook.close()
        }
    }

    private data class ImportColumns(
        val title: Int = 0,
        val username: Int = 1,
        val password: Int = 2,
        val category: Int = 3,
        val expiryDate: Int = 4,
        val email: Int = 5,
        val mobileNumber: Int = 6,
        val description: Int = 7,
        val userIds: Int? = 8,
        val teamIds: Int? = 9
    )

    private fun resolveImportColumns(headerRow: Row?): ImportColumns {
        if (headerRow == null) {
            return ImportColumns()
        }

        val headerMap = mutableMapOf<String, Int>()
        for (cell in headerRow) {
            val key = normalizeImportHeader(cellValue(cell))
            if (key.isNotEmpty()) {
                headerMap[key] = cell.columnIndex
            }
        }

        if ("title" !in headerMap && "username" !in headerMap) {
            return ImportColumns()
        }

        fun col(vararg keys: String, default: Int): Int =
            keys.firstNotNullOfOrNull { headerMap[it] } ?: default

        fun colOptional(vararg keys: String): Int? =
            keys.firstNotNullOfOrNull { headerMap[it] }

        val userIdsCol = colOptional("user_ids", "user_id", "userid") ?: 8
        val teamIdsCol =
            colOptional("team_ids", "team_id", "teamid")
                ?: if (userIdsCol != 9) 9 else null

        return ImportColumns(
            title = col("title", default = 0),
            username = col("username", "account_username", default = 1),
            password = col("password", default = 2),
            category = col("category", default = 3),
            expiryDate = col("expiry_date", "expiry", default = 4),
            email = col("email", default = 5),
            mobileNumber = col("mobile_number", "mobile", default = 6),
            description = col("description", default = 7),
            userIds = userIdsCol,
            teamIds = teamIdsCol
        )
    }

    private fun normalizeImportHeader(raw: String): String =
        raw.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')

    private fun applyImportAssignments(
        rowIndex: Int,
        passwordId: Long,
        userIdsCell: Cell?,
        teamIdsCell: Cell?,
        syncUsers: Boolean,
        syncTeams: Boolean
    ) {
        val rowNumber = rowIndex + 1
        val userIds = if (syncUsers) parseIdListFromCell(userIdsCell) else emptyList()
        val teamIds = if (syncTeams) parseIdListFromCell(teamIdsCell) else emptyList()

        if (!syncUsers && !syncTeams) {
            return
        }

        if (syncUsers) {
            val userIdsRaw = cellValue(userIdsCell)
            if (userIdsRaw.isNotBlank() && userIds.isEmpty()) {
                throw RuntimeException(
                    "Row $rowNumber: Invalid User IDs '$userIdsRaw'. Use comma-separated numbers (e.g. 1, 2)."
                )
            }
            if (userIds.isNotEmpty()) {
                val foundUserIds =
                    userRepository.findAllById(userIds).mapNotNull { it.id }.toSet()
                val missingUsers = userIds.toSet() - foundUserIds
                if (missingUsers.isNotEmpty()) {
                    throw RuntimeException(
                        "Row $rowNumber: User(s) not found: ${missingUsers.sorted().joinToString(", ")}"
                    )
                }
            }
            assignmentService.syncAssignments(passwordId, userIds.toSet())
        }

        if (syncTeams) {
            val teamIdsRaw = cellValue(teamIdsCell)
            if (teamIdsRaw.isNotBlank() && teamIds.isEmpty()) {
                throw RuntimeException(
                    "Row $rowNumber: Invalid Team IDs '$teamIdsRaw'. Use comma-separated numbers (e.g. 1, 2)."
                )
            }
            teamPasswordAssignmentSyncService.replaceAssignmentsForPassword(
                passwordId,
                teamIds.toSet()
            )
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

    private fun parseIdListFromCell(cell: Cell?): List<Long> {
        if (cell == null) return emptyList()
        return when (cell.cellType) {
            CellType.NUMERIC -> listOfNotNull(parseNumericId(cell.numericCellValue))
            CellType.STRING -> parseIdList(cell.stringCellValue)
            CellType.FORMULA ->
                when (cell.cachedFormulaResultType) {
                    CellType.NUMERIC -> listOfNotNull(parseNumericId(cell.numericCellValue))
                    CellType.STRING -> parseIdList(cell.stringCellValue)
                    else -> parseIdList(cellFormatter.formatCellValue(cell))
                }
            else -> parseIdList(cellFormatter.formatCellValue(cell))
        }
    }

    private fun parseNumericId(value: Double): Long? {
        if (value < 0 || value % 1.0 != 0.0) return null
        return value.toLong()
    }

    private fun parseIdList(raw: String): List<Long> =
        raw.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { parseIdToken(it) }

    private fun parseIdToken(token: String): Long? {
        token.toLongOrNull()?.let { return it }
        token.toDoubleOrNull()?.let { value ->
            return parseNumericId(value)
        }
        return null
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
                "Expiry Date",
                "Email",
                "Mobile Number",
                "Description",
                "user_id",
                "team_id",
                "users",
                "teams"
            )

        val headerRow =
            sheet.createRow(0)

        headers.forEachIndexed { index, title ->
            headerRow.createCell(index).setCellValue(title)
        }

        val formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd")

        entries.forEachIndexed { index, entry ->
            val row = sheet.createRow(index + 1)
            val entryId = entry.id

            val userIds =
                entryId
                    ?.let { assignmentService.assignedUserIdsForEntry(it).sorted() }
                    ?: emptyList()
            val teamIds =
                entryId
                    ?.let { findTeamIdsForPassword(it).sorted() }
                    ?: emptyList()
            val userLabels =
                if (userIds.isEmpty()) {
                    ""
                } else {
                    userRepository.findAllById(userIds)
                        .mapNotNull { user ->
                            user.username?.let { username ->
                                user.email?.takeIf { it.isNotBlank() }
                                    ?.let { email -> "$username ($email)" }
                                    ?: username
                            }
                        }
                        .sorted()
                        .joinToString(", ")
                }
            val teamLabels =
                if (teamIds.isEmpty()) {
                    ""
                } else {
                    teamRepository.findAllById(teamIds)
                        .map { it.name }
                        .sorted()
                        .joinToString(", ")
                }

            row.createCell(0).setCellValue(entry.title)
            row.createCell(1).setCellValue(entry.username)
            row.createCell(2).setCellValue(entry.password)
            row.createCell(3).setCellValue(entry.category?.name ?: "")
            row.createCell(4).setCellValue(
                entry.expiryDate?.toLocalDate()?.format(formatter) ?: ""
            )
            row.createCell(5).setCellValue(entry.email ?: "")
            row.createCell(6).setCellValue(entry.mobileNumber ?: "")
            row.createCell(7).setCellValue(entry.description)
            row.createCell(8).setCellValue(userIds.joinToString(", "))
            row.createCell(9).setCellValue(teamIds.joinToString(", "))
            row.createCell(10).setCellValue(userLabels)
            row.createCell(11).setCellValue(teamLabels)
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