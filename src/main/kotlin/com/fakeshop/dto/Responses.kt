package com.fakeshop.dto

import com.fakeshop.domain.Product
import com.fakeshop.domain.Review
import java.math.BigDecimal
import java.time.OffsetDateTime

data class DimensionsDto(
    val width: BigDecimal?,
    val height: BigDecimal?,
    val depth: BigDecimal?,
)

data class MetaDto(
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
    val barcode: String?,
    val qrCode: String?,
)

data class ReviewResponse(
    val id: Long?,
    val rating: Int,
    val comment: String?,
    val pros: String?,
    val cons: String?,
    val deliveryRating: Int?,
    val wouldRecommend: Boolean?,
    val date: OffsetDateTime,
    val reviewerName: String?,
    val reviewerEmail: String?,
) {
    companion object {
        fun from(review: Review) = ReviewResponse(
            id = review.id,
            rating = review.rating,
            comment = review.comment,
            pros = review.pros,
            cons = review.cons,
            deliveryRating = review.deliveryRating,
            wouldRecommend = review.wouldRecommend,
            date = review.reviewDate,
            reviewerName = review.reviewerName,
            reviewerEmail = review.reviewerEmail,
        )
    }
}

/** Rövid termékadat listázáshoz. */
data class ProductSummaryResponse(
    val id: Long,
    val title: String,
    val category: String,
    val price: BigDecimal,
    val discountPercentage: BigDecimal,
    val rating: BigDecimal,
    val stock: Int,
    val brand: String?,
    val thumbnail: String?,
) {
    companion object {
        fun from(p: Product) = ProductSummaryResponse(
            id = p.id,
            title = p.title,
            category = p.category,
            price = p.price,
            discountPercentage = p.discountPercentage,
            rating = p.rating,
            stock = p.stock,
            brand = p.brand,
            thumbnail = p.thumbnail,
        )
    }
}

/** Teljes termékadat egyetlen termék lekérdezéséhez. */
data class ProductResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val category: String,
    val price: BigDecimal,
    val discountPercentage: BigDecimal,
    val rating: BigDecimal,
    val stock: Int,
    val tags: List<String>,
    val brand: String?,
    val sku: String?,
    val weight: Int?,
    val dimensions: DimensionsDto,
    val warrantyInformation: String?,
    val shippingInformation: String?,
    val availabilityStatus: String?,
    val returnPolicy: String?,
    val minimumOrderQuantity: Int?,
    val meta: MetaDto,
    val images: List<String>,
    val thumbnail: String?,
    val reviews: List<ReviewResponse>,
) {
    companion object {
        fun from(p: Product) = ProductResponse(
            id = p.id,
            title = p.title,
            description = p.description,
            category = p.category,
            price = p.price,
            discountPercentage = p.discountPercentage,
            rating = p.rating,
            stock = p.stock,
            tags = p.tags.sorted(),
            brand = p.brand,
            sku = p.sku,
            weight = p.weight,
            dimensions = DimensionsDto(p.dimensions.width, p.dimensions.height, p.dimensions.depth),
            warrantyInformation = p.warrantyInformation,
            shippingInformation = p.shippingInformation,
            availabilityStatus = p.availabilityStatus,
            returnPolicy = p.returnPolicy,
            minimumOrderQuantity = p.minimumOrderQuantity,
            meta = MetaDto(p.metaCreatedAt, p.metaUpdatedAt, p.barcode, p.qrCode),
            images = p.images.toList(),
            thumbnail = p.thumbnail,
            reviews = p.reviews.map { ReviewResponse.from(it) },
        )
    }
}

/** Lapozott válasz a dummyjson stílusában (products / total / skip / limit). */
data class PagedResponse<T>(
    val products: List<T>,
    val total: Long,
    val skip: Int,
    val limit: Int,
)
