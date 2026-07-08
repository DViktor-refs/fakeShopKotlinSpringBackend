package com.fakeshop.dto

import com.fakeshop.domain.Order
import com.fakeshop.domain.OrderItem
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.OffsetDateTime

// ---------- Kosár ----------

data class AddCartItemRequest(
    @field:Min(value = 1, message = "A productId kötelező")
    val productId: Long,

    @field:Min(value = 1, message = "A quantity legalább 1 legyen")
    val quantity: Int = 1,
)

data class UpdateCartItemRequest(
    @field:Min(value = 1, message = "A quantity legalább 1 legyen")
    val quantity: Int,
)

data class CartItemResponse(
    val productId: Long,
    val title: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val lineTotal: BigDecimal,
    val availableStock: Int,
)

data class CartResponse(
    val items: List<CartItemResponse>,
    val totalItems: Int,
    val totalAmount: BigDecimal,
)

// ---------- Rendelés ----------

data class OrderItemResponse(
    val productId: Long,
    val productTitle: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val lineTotal: BigDecimal,
) {
    companion object {
        fun from(item: OrderItem) = OrderItemResponse(
            productId = item.productId,
            productTitle = item.productTitle,
            unitPrice = item.unitPrice,
            quantity = item.quantity,
            lineTotal = item.lineTotal,
        )
    }
}

data class OrderResponse(
    val id: Long?,
    val status: String,
    val totalAmount: BigDecimal,
    val items: List<OrderItemResponse>,
    val paymentStatus: String,
    val paymentId: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    companion object {
        fun from(order: Order) = OrderResponse(
            id = order.id,
            status = order.status.name,
            totalAmount = order.totalAmount,
            items = order.items.map { OrderItemResponse.from(it) },
            paymentStatus = order.paymentStatus.name,
            paymentId = order.paymentId,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt,
        )
    }
}

data class UpdateOrderStatusRequest(
    @field:NotBlank(message = "A status kötelező (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)")
    val status: String,
)

// ---------- Fizetés (Barion) ----------

/** A /pay végpont válasza: ide kell irányítani a vásárlót a fizetéshez. */
data class StartPaymentResponse(
    val orderId: Long,
    val paymentId: String,
    val gatewayUrl: String,
)
