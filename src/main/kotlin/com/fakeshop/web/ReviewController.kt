package com.fakeshop.web

import com.fakeshop.dto.PagedResponse
import com.fakeshop.dto.ReviewCreateRequest
import com.fakeshop.dto.ReviewResponse
import com.fakeshop.service.ReviewService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products/{productId}/reviews")
class ReviewController(
    private val reviewService: ReviewService,
) {

    /** Egy termékhez tartozó vélemények listája (lapozva, legújabb elöl). */
    @GetMapping
    fun list(
        @PathVariable productId: Long,
        @RequestParam(defaultValue = "30") limit: Int,
        @RequestParam(defaultValue = "0") skip: Int,
    ): PagedResponse<ReviewResponse> =
        reviewService.listForProduct(productId, limit, skip.coerceAtLeast(0))

    /** Új vélemény hozzáadása egy termékhez. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun add(
        @PathVariable productId: Long,
        @Valid @RequestBody request: ReviewCreateRequest,
    ): ReviewResponse = reviewService.addReview(productId, request)
}
