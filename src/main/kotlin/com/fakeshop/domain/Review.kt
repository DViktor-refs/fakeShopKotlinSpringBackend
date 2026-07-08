package com.fakeshop.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * Egy termékhez tartozó vélemény. A dummyjson "reviews" tömbjének egy eleme.
 * Külön táblában tárolva, hogy később bővíthető és lekérdezhető legyen.
 */
@Entity
@Table(name = "reviews")
class Review(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null,

    @Column(name = "rating", nullable = false)
    var rating: Int = 0,

    @Column(name = "comment", length = 1000)
    var comment: String? = null,

    /** Mi tetszett a termékben (a értékelő emailből). */
    @Column(name = "pros", length = 1000)
    var pros: String? = null,

    /** Mi nem tetszett a termékben (az értékelő emailből). */
    @Column(name = "cons", length = 1000)
    var cons: String? = null,

    /** A szállítás értékelése 1–5 (opcionális, az értékelő emailből). */
    @Column(name = "delivery_rating")
    var deliveryRating: Int? = null,

    /** Ajánlaná-e másnak a terméket (opcionális, az értékelő emailből). */
    @Column(name = "would_recommend")
    var wouldRecommend: Boolean? = null,

    @Column(name = "review_date", nullable = false)
    var reviewDate: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "reviewer_name", length = 150)
    var reviewerName: String? = null,

    @Column(name = "reviewer_email", length = 200)
    var reviewerEmail: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
)
