package com.fakeshop.dto

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Termék módosítása. Csak a price, discountPercentage, rating és stock írható.
 * Minden mező opcionális (PATCH szemantika): csak a megadottak frissülnek.
 */
data class ProductUpdateRequest(
    @field:DecimalMin(value = "0.0", inclusive = true, message = "A price nem lehet negatív")
    val price: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", inclusive = true, message = "A discountPercentage 0 és 100 között lehet")
    @field:DecimalMax(value = "100.0", inclusive = true, message = "A discountPercentage 0 és 100 között lehet")
    val discountPercentage: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", inclusive = true, message = "A rating 0 és 5 között lehet")
    @field:DecimalMax(value = "5.0", inclusive = true, message = "A rating 0 és 5 között lehet")
    val rating: BigDecimal? = null,

    @field:Min(value = 0, message = "A stock nem lehet negatív")
    val stock: Int? = null,
) {
    fun isEmpty(): Boolean =
        price == null && discountPercentage == null && rating == null && stock == null
}

/** Új vélemény létrehozása egy termékhez. */
data class ReviewCreateRequest(
    @field:Min(value = 1, message = "A rating 1 és 5 között lehet")
    @field:Max(value = 5, message = "A rating 1 és 5 között lehet")
    val rating: Int,

    @field:Size(max = 1000, message = "A comment legfeljebb 1000 karakter lehet")
    val comment: String? = null,

    @field:NotBlank(message = "A reviewerName kötelező")
    @field:Size(max = 150)
    val reviewerName: String,

    @field:Email(message = "Érvénytelen e-mail cím")
    @field:Size(max = 200)
    val reviewerEmail: String? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val date: OffsetDateTime? = null,
)
