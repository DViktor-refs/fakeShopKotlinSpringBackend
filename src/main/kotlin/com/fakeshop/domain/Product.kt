package com.fakeshop.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * A bolt központi entitása. A dummyjson /products egy elemének felel meg.
 *
 * Szerkeszthető mezők (lásd PATCH végpont): price, discountPercentage, rating, stock.
 * Minden más mező csak olvasható az API-n keresztül.
 */
@Entity
@Table(name = "products")
class Product(

    @Id
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "title", nullable = false)
    var title: String = "",

    @Column(name = "description", columnDefinition = "text")
    var description: String? = null,

    @Column(name = "category", nullable = false, length = 100)
    var category: String = "",

    // ---- Szerkeszthető mezők ----
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO,

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    var discountPercentage: BigDecimal = BigDecimal.ZERO,

    @Column(name = "rating", nullable = false, precision = 3, scale = 2)
    var rating: BigDecimal = BigDecimal.ZERO,

    @Column(name = "stock", nullable = false)
    var stock: Int = 0,
    // ------------------------------

    @Column(name = "brand", length = 150)
    var brand: String? = null,

    @Column(name = "sku", length = 100)
    var sku: String? = null,

    @Column(name = "weight")
    var weight: Int? = null,

    @Embedded
    var dimensions: Dimensions = Dimensions(),

    @Column(name = "warranty_information")
    var warrantyInformation: String? = null,

    @Column(name = "shipping_information")
    var shippingInformation: String? = null,

    @Column(name = "availability_status", length = 50)
    var availabilityStatus: String? = null,

    @Column(name = "return_policy")
    var returnPolicy: String? = null,

    @Column(name = "minimum_order_quantity")
    var minimumOrderQuantity: Int? = null,

    @Column(name = "barcode", length = 100)
    var barcode: String? = null,

    @Column(name = "qr_code", length = 512)
    var qrCode: String? = null,

    @Column(name = "meta_created_at")
    var metaCreatedAt: OffsetDateTime? = null,

    @Column(name = "meta_updated_at")
    var metaUpdatedAt: OffsetDateTime? = null,

    @Column(name = "thumbnail", length = 512)
    var thumbnail: String? = null,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_tags", joinColumns = [JoinColumn(name = "product_id")])
    @Column(name = "tag", length = 100)
    var tags: MutableSet<String> = mutableSetOf(),

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_images", joinColumns = [JoinColumn(name = "product_id")])
    @OrderColumn(name = "position")
    @Column(name = "image_url", length = 512)
    var images: MutableList<String> = mutableListOf(),

    @OneToMany(
        mappedBy = "product",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    @OrderBy("reviewDate DESC")
    var reviews: MutableList<Review> = mutableListOf(),

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
    fun addReview(review: Review) {
        review.product = this
        reviews.add(review)
    }
}
