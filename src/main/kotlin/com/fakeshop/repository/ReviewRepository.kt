package com.fakeshop.repository

import com.fakeshop.domain.Review
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReviewRepository : JpaRepository<Review, Long> {
    fun findByProductId(productId: Long, pageable: Pageable): Page<Review>
    fun countByProductId(productId: Long): Long
}
