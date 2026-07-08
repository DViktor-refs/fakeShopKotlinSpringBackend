package com.fakeshop.web

import com.fakeshop.payment.BarionApiException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

data class ApiError(
    val timestamp: OffsetDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val fieldErrors: Map<String, String?> = emptyMap(),
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ApiError> =
        build(HttpStatus.NOT_FOUND, ex.message ?: "Az erőforrás nem található")

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException): ResponseEntity<ApiError> =
        build(HttpStatus.UNAUTHORIZED, ex.message ?: "Hibás belépési adatok")

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(ex: InsufficientStockException): ResponseEntity<ApiError> =
        build(HttpStatus.CONFLICT, ex.message ?: "Nincs elég készlet")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiError> =
        build(HttpStatus.BAD_REQUEST, ex.message ?: "Hibás kérés")

    @ExceptionHandler(BarionApiException::class)
    fun handleBarionError(ex: BarionApiException): ResponseEntity<ApiError> =
        build(HttpStatus.BAD_GATEWAY, ex.message ?: "A Barion fizetési szolgáltatás hibát adott vissza")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        val body = ApiError(
            timestamp = OffsetDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Érvénytelen kérés mező(k)",
            fieldErrors = fieldErrors,
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    private fun build(status: HttpStatus, message: String): ResponseEntity<ApiError> {
        val body = ApiError(
            timestamp = OffsetDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
        )
        return ResponseEntity.status(status).body(body)
    }
}
