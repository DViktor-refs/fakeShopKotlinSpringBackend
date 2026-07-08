package com.fakeshop.web

import com.fakeshop.dto.OrderResponse
import com.fakeshop.dto.UpdateOrderStatusRequest
import com.fakeshop.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Rendelések. Minden végpont tokent igényel; az állapotmódosítás admin jogot.
 */
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService,
) {

    /** Pénztár: a kosárból rendelést készít és levonja a készletet. */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    fun checkout(authentication: Authentication): OrderResponse =
        orderService.checkout(authentication.name)

    /** A bejelentkezett felhasználó rendelései (legújabb elöl). */
    @GetMapping
    fun myOrders(authentication: Authentication): List<OrderResponse> =
        orderService.myOrders(authentication.name)

    /** Egy saját rendelés részletei. */
    @GetMapping("/{id}")
    fun myOrder(
        authentication: Authentication,
        @PathVariable id: Long,
    ): OrderResponse = orderService.myOrder(authentication.name, id)

    /** Saját rendelés lemondása (a készlet visszaíródik). */
    @PostMapping("/{id}/cancel")
    fun cancel(
        authentication: Authentication,
        @PathVariable id: Long,
    ): OrderResponse = orderService.cancel(authentication.name, id)

    /** Admin: rendelés állapotának módosítása. */
    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateOrderStatusRequest,
    ): OrderResponse = orderService.updateStatus(id, request.status)
}
