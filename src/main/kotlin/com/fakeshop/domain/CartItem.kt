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

/**
 * Kosártétel. A termékre csak azonosítóval hivatkozik; az árat mindig
 * a termék aktuális ára adja (a kosár a friss árat mutatja, nem pillanatképet).
 */
@Entity
@Table(name = "cart_items")
class CartItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    var cart: Cart? = null,

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0,

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 1,
)
