package com.management.services

class DuplicatePasswordEntryException(
    message: String = "This title and account username are already exist."
) : RuntimeException(message)
