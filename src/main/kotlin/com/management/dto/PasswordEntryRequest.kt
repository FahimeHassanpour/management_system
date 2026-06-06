package com.management.dto

data class PasswordEntryRequest(
    val title: String,
    val username: String,
    val password: String,
    val description: String = "",
    val email: String? = null,
    val mobileNumber: String? = null,
    val categoryId: Long? = null,
    val userIds: List<Long> = emptyList(),
    val expiryDate: String? = null,
    var teamIds: List<Long> = emptyList()
)