package com.fakeshop.service

import com.fakeshop.domain.Cart
import com.fakeshop.domain.CartItem
import com.fakeshop.dto.CartItemResponse
import com.fakeshop.dto.CartResponse
import com.fakeshop.repository.CartRepository
import com.fakeshop.repository.ProductRepository
import com.fakeshop.web.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val currentUserService: CurrentUserService,
) {

    @Transactional(readOnly = true)
    fun getCart(username: String): CartResponse {
        val user = currentUserService.require(username)
        val cart = cartRepository.findByUserId(user.id!!)
        return toResponse(cart)
    }

    @Transactional
    fun addItem(username: String, productId: Long, quantity: Int): CartResponse {
        val user = currentUserService.require(username)
        // A termék létezésének ellenőrzése
        productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Nincs ilyen azonosítójú termék: $productId") }

        val cart = getOrCreateCart(user.id!!)
        val existing = cart.items.firstOrNull { it.productId == productId }
        if (existing != null) {
            existing.quantity += quantity
        } else {
            cart.addItem(CartItem(productId = productId, quantity = quantity))
        }
        cart.updatedAt = OffsetDateTime.now()
        val saved = cartRepository.save(cart)
        return toResponse(saved)
    }

    @Transactional
    fun updateItem(username: String, productId: Long, quantity: Int): CartResponse {
        val user = currentUserService.require(username)
        val cart = cartRepository.findByUserId(user.id!!)
            ?: throw ResourceNotFoundException("A kosár üres")
        val item = cart.items.firstOrNull { it.productId == productId }
            ?: throw ResourceNotFoundException("Ez a termék nincs a kosárban: $productId")
        item.quantity = quantity
        cart.updatedAt = OffsetDateTime.now()
        val saved = cartRepository.save(cart)
        return toResponse(saved)
    }

    @Transactional
    fun removeItem(username: String, productId: Long): CartResponse {
        val user = currentUserService.require(username)
        val cart = cartRepository.findByUserId(user.id!!)
            ?: throw ResourceNotFoundException("A kosár üres")
        cart.items.removeIf { it.productId == productId }
        cart.updatedAt = OffsetDateTime.now()
        val saved = cartRepository.save(cart)
        return toResponse(saved)
    }

    @Transactional
    fun clear(username: String): CartResponse {
        val user = currentUserService.require(username)
        val cart = cartRepository.findByUserId(user.id!!) ?: return emptyCart()
        cart.items.clear()
        cart.updatedAt = OffsetDateTime.now()
        cartRepository.save(cart)
        return emptyCart()
    }

    private fun getOrCreateCart(userId: Long): Cart =
        cartRepository.findByUserId(userId) ?: cartRepository.save(Cart(userId = userId))

    private fun toResponse(cart: Cart?): CartResponse {
        if (cart == null || cart.items.isEmpty()) return emptyCart()

        val products = productRepository.findAllById(cart.items.map { it.productId })
            .associateBy { it.id }

        val itemResponses = cart.items.mapNotNull { item ->
            val product = products[item.productId] ?: return@mapNotNull null
            val lineTotal = product.price.multiply(BigDecimal(item.quantity))
            CartItemResponse(
                productId = item.productId,
                title = product.title,
                unitPrice = product.price,
                quantity = item.quantity,
                lineTotal = lineTotal,
                availableStock = product.stock,
            )
        }
        val total = itemResponses.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.lineTotal) }
        return CartResponse(
            items = itemResponses,
            totalItems = itemResponses.sumOf { it.quantity },
            totalAmount = total,
        )
    }

    private fun emptyCart() = CartResponse(items = emptyList(), totalItems = 0, totalAmount = BigDecimal.ZERO)
}
