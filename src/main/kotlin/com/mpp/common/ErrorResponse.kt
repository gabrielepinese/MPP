package com.mpp.common

import java.time.Instant

data class FieldError(
    val field: String,
    val message: String
)

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val fieldErrors: List<FieldError> = emptyList(),
    val timestamp: Instant = Instant.now()
)
