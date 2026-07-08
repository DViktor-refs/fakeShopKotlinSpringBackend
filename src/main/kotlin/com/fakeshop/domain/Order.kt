package com.fakeshop.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Egy leadott rendelés. A tételek a leadás pillanatában rögzített
 * (pillanatkép) árat és terméknevet tárolják, hogy a későbbi ármódosítás
 * ne írja felül a rendelés értékét.
 */
@Entity(name = "ShopOrder")
@Table(name = "orders")
class Order(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    var paymentStatus: PaymentStatus = PaymentStatus.NOT_STARTED,

    /** A Barion által adott fizetés-azonosító (PaymentId, GUID string alakban). */
    @Column(name = "payment_id", length = 64)
    var paymentId: String? = null,

    /** Az általunk generált, egyedi PaymentRequestId, amit a Barionnak küldtünk. */
    @Column(name = "payment_request_id", length = 64)
    var paymentRequestId: String? = null,

    /** A Barion legutóbb lekérdezett nyers állapota (pl. "Succeeded"), diagnosztikához. */
    @Column(name = "barion_status", length = 30)
    var barionStatus: String? = null,

    @OneToMany(
        mappedBy = "order",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    @OrderBy("id ASC")
    var items: MutableList<OrderItem> = mutableListOf(),

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
) {
    fun addItem(item: OrderItem) {
        item.order = this
        items.add(item)
    }
}
