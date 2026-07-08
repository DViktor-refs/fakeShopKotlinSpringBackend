package com.fakeshop.seed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * A dummyjson /products válaszának leképezése a betöltéshez.
 * Az ismeretlen mezőket szándékosan figyelmen kívül hagyjuk.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DummyProductList(
    val products: List<DummyProduct> = emptyList(),
    val total: Int = 0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DummyProduct(
    val id: Long = 0,
    val title: String = "",
    val description: String? = null,
    val category: String = "",
    val price: BigDecimal = BigDecimal.ZERO,
    val discountPercentage: BigDecimal = BigDecimal.ZERO,
    val rating: BigDecimal = BigDecimal.ZERO,
    val stock: Int = 0,
    val tags: List<String> = emptyList(),
    val brand: String? = null,
    val sku: String? = null,
    val weight: Int? = null,
    val dimensions: DummyDimensions? = null,
    val warrantyInformation: String? = null,
    val shippingInformation: String? = null,
    val availabilityStatus: String? = null,
    val returnPolicy: String? = null,
    val minimumOrderQuantity: Int? = null,
    val meta: DummyMeta? = null,
    val images: List<String> = emptyList(),
    val thumbnail: String? = null,
    val reviews: List<DummyReview> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DummyDimensions(
    val width: BigDecimal? = null,
    val height: BigDecimal? = null,
    val depth: BigDecimal? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DummyMeta(
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
    val barcode: String? = null,
    val qrCode: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DummyReview(
    val rating: Int = 0,
    val comment: String? = null,
    val date: OffsetDateTime? = null,
    val reviewerName: String? = null,
    val reviewerEmail: String? = null,
)
