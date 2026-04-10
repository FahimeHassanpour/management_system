package com.management.dto

data class PasswordEntryRequest(
    val title: String,
    val username: String,
    val password: String,
    val description: String = "",
    val categoryId: Long? = null,
    val userIds: List<Long> = emptyList()
)
