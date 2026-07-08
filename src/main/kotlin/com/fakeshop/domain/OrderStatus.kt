package com.fakeshop.domain

/**
 * A rendelés életciklusa (fizetés nélkül).
 * PENDING -> CONFIRMED -> SHIPPED -> DELIVERED
 * Bármelyik nem-terminális állapotból lehet CANCELLED (a készlet visszaíródik).
 */
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
}
