package com.fakeshop.web

import com.fakeshop.dto.AddCartItemRequest
import com.fakeshop.dto.CartResponse
import com.fakeshop.dto.UpdateCartItemRequest
import com.fakeshop.service.CartService
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * A bejelentkezett felhasználó kosara. Minden végpont tokent igényel.
 */
@RestController
@RequestMapping("/api/cart")
class CartController(
    private val cartService: CartService,
) {

    /** A kosár tartalma (aktuális árakkal). */
    @GetMapping
    fun getCart(authentication: Authentication): CartResponse =
        cartService.getCart(authentication.name)

    /** Termék hozzáadása a kosárhoz (ha már benne van, a mennyiség nő). */
    @PostMapping("/items")
    fun addItem(
        authentication: Authentication,
        @Valid @RequestBody request: AddCartItemRequest,
    ): CartResponse = cartService.addItem(authentication.name, request.productId, request.quantity)

    /** Egy kosártétel mennyiségének módosítása. */
    @PutMapping("/items/{productId}")
    fun updateItem(
        authentication: Authentication,
        @PathVariable productId: Long,
        @Valid @RequestBody request: UpdateCartItemRequest,
    ): CartResponse = cartService.updateItem(authentication.name, productId, request.quantity)

    /** Egy termék eltávolítása a kosárból. */
    @DeleteMapping("/items/{productId}")
    fun removeItem(
        authentication: Authentication,
        @PathVariable productId: Long,
    ): CartResponse = cartService.removeItem(authentication.name, productId)

    /** A teljes kosár kiürítése. */
    @DeleteMapping
    fun clear(authentication: Authentication): CartResponse =
        cartService.clear(authentication.name)
}
