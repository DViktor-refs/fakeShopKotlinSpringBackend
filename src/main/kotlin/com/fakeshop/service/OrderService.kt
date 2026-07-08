package com.fakeshop.service

import com.fakeshop.domain.Order
import com.fakeshop.domain.OrderItem
import com.fakeshop.domain.OrderStatus
import com.fakeshop.dto.OrderResponse
import com.fakeshop.repository.CartRepository
import com.fakeshop.repository.OrderRepository
import com.fakeshop.repository.ProductRepository
import com.fakeshop.web.InsufficientStockException
import com.fakeshop.web.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val currentUserService: CurrentUserService,
) {

    /**
     * Pénztár: a kosárból rendelést készít.
     * Minden tételnél atomian levonja a készletet; ha bármelyiknél nincs elég,
     * az egész művelet visszagördül (semmi nem változik).
     */
    @Transactional
    fun checkout(username: String): OrderResponse {
        val user = currentUserService.require(username)
        val cart = cartRepository.findByUserId(user.id!!)
        if (cart == null || cart.items.isEmpty()) {
            throw IllegalArgumentException("A kosár üres, nincs mit megrendelni")
        }

        val products = productRepository.findAllById(cart.items.map { it.productId })
            .associateBy { it.id }

        val order = Order(userId = user.id!!, status = OrderStatus.PENDING)
        var total = BigDecimal.ZERO

        for (item in cart.items) {
            val product = products[item.productId]
                ?: throw ResourceNotFoundException("A kosárban lévő termék már nem létezik: ${item.productId}")

            val affected = productRepository.decrementStock(item.productId, item.quantity)
            if (affected == 0) {
                throw InsufficientStockException(
                    "Nincs elég készlet a(z) \"${product.title}\" termékből (kért: ${item.quantity}, elérhető: ${product.stock})"
                )
            }

            val unitPrice = product.price
            val lineTotal = unitPrice.multiply(BigDecimal(item.quantity))
            total = total.add(lineTotal)

            order.addItem(
                OrderItem(
                    productId = product.id,
                    productTitle = product.title,
                    unitPrice = unitPrice,
                    quantity = item.quantity,
                    lineTotal = lineTotal,
                )
            )
        }

        order.totalAmount = total
        val saved = orderRepository.save(order)

        // A kosár kiürítése a sikeres rendelés után
        cart.items.clear()
        cart.updatedAt = OffsetDateTime.now()
        cartRepository.save(cart)

        return OrderResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun myOrders(username: String): List<OrderResponse> {
        val user = currentUserService.require(username)
        return orderRepository.findByUserIdOrderByIdDesc(user.id!!).map { OrderResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun myOrder(username: String, orderId: Long): OrderResponse {
        val user = currentUserService.require(username)
        val order = orderRepository.findByIdAndUserId(orderId, user.id!!)
            ?: throw ResourceNotFoundException("Nincs ilyen rendelés: $orderId")
        return OrderResponse.from(order)
    }

    /**
     * A felhasználó lemondja a saját rendelését. Csak PENDING vagy CONFIRMED
     * állapotban lehet; ilyenkor a készlet visszaíródik.
     */
    @Transactional
    fun cancel(username: String, orderId: Long): OrderResponse {
        val user = currentUserService.require(username)
        val order = orderRepository.findByIdAndUserId(orderId, user.id!!)
            ?: throw ResourceNotFoundException("Nincs ilyen rendelés: $orderId")

        if (order.status != OrderStatus.PENDING && order.status != OrderStatus.CONFIRMED) {
            throw IllegalArgumentException("Ez a rendelés már nem mondható le (állapot: ${order.status})")
        }

        restoreStock(order)
        order.status = OrderStatus.CANCELLED
        order.updatedAt = OffsetDateTime.now()
        return OrderResponse.from(orderRepository.save(order))
    }

    /**
     * Admin művelet: rendelés állapotának módosítása.
     * CANCELLED-re állításkor a készlet visszaíródik. Terminális állapotból
     * (DELIVERED, CANCELLED) nincs továbblépés.
     */
    @Transactional
    fun updateStatus(orderId: Long, newStatusRaw: String): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Nincs ilyen rendelés: $orderId") }

        val newStatus = try {
            OrderStatus.valueOf(newStatusRaw.trim().uppercase())
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException("Ismeretlen állapot: $newStatusRaw")
        }

        if (order.status == OrderStatus.DELIVERED || order.status == OrderStatus.CANCELLED) {
            throw IllegalArgumentException("A rendelés már lezárt állapotban van (${order.status}), nem módosítható")
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restoreStock(order)
        }
        order.status = newStatus
        order.updatedAt = OffsetDateTime.now()
        return OrderResponse.from(orderRepository.save(order))
    }

    private fun restoreStock(order: Order) {
        for (item in order.items) {
            productRepository.incrementStock(item.productId, item.quantity)
        }
    }
}
