package com.fakeshop.repository

import com.fakeshop.domain.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findByUserIdOrderByIdDesc(userId: Long): List<Order>
    fun findByIdAndUserId(id: Long, userId: Long): Order?
    fun findByPaymentId(paymentId: String): Order?
}
