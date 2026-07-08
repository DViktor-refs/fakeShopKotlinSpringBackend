package com.fakeshop.service

import com.fakeshop.domain.Review
import com.fakeshop.dto.PagedResponse
import com.fakeshop.dto.ReviewCreateRequest
import com.fakeshop.dto.ReviewResponse
import com.fakeshop.repository.ProductRepository
import com.fakeshop.repository.ReviewRepository
import com.fakeshop.web.ResourceNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ReviewService(
    private val productRepository: ProductRepository,
    private val reviewRepository: ReviewRepository,
) {

    @Transactional(readOnly = true)
    fun listForProduct(productId: Long, limit: Int, skip: Int): PagedResponse<ReviewResponse> {
        if (!productRepository.existsById(productId)) {
            throw ResourceNotFoundException("Nincs ilyen azonosítójú termék: $productId")
        }
        val safeLimit = limit.coerceIn(1, 100)
        val page = skip / safeLimit
        val pageable = PageRequest.of(page, safeLimit, Sort.by(Sort.Direction.DESC, "reviewDate"))
        val result = reviewRepository.findByProductId(productId, pageable)
        return PagedResponse(
            products = result.content.map { ReviewResponse.from(it) },
            total = result.totalElements,
            skip = skip,
            limit = safeLimit,
        )
    }

    @Transactional
    fun addReview(productId: Long, request: ReviewCreateRequest): ReviewResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Nincs ilyen azonosítójú termék: $productId") }

        val review = Review(
            rating = request.rating,
            comment = request.comment,
            reviewerName = request.reviewerName,
            reviewerEmail = request.reviewerEmail,
            reviewDate = request.date ?: OffsetDateTime.now(),
        )
        product.addReview(review)
        productRepository.save(product)
        return ReviewResponse.from(review)
    }
}
