package com.fakeshop.service

import com.fakeshop.domain.Product
import com.fakeshop.dto.PagedResponse
import com.fakeshop.dto.ProductResponse
import com.fakeshop.dto.ProductSummaryResponse
import com.fakeshop.dto.ProductUpdateRequest
import com.fakeshop.repository.ProductRepository
import com.fakeshop.repository.ProductSpecifications
import com.fakeshop.web.ResourceNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
class ProductService(
    private val productRepository: ProductRepository,
) {

    /** Engedélyezett rendezési mezők (a kívülről jövő érték nem mehet közvetlenül a DB-be). */
    private val sortableFields = setOf("id", "title", "price", "rating", "stock", "discountPercentage", "category")

    @Transactional(readOnly = true)
    fun search(criteria: ProductQuery): PagedResponse<ProductSummaryResponse> {
        val sortField = if (criteria.sortBy in sortableFields) criteria.sortBy else "id"
        val direction = if (criteria.order.equals("desc", ignoreCase = true)) Sort.Direction.DESC else Sort.Direction.ASC

        val page = (criteria.skip / criteria.limit.coerceAtLeast(1))
        val pageable = PageRequest.of(page, criteria.limit, Sort.by(direction, sortField))

        val spec = ProductSpecifications.combine(
            listOf(
                ProductSpecifications.search(criteria.q),
                ProductSpecifications.category(criteria.category),
                ProductSpecifications.brand(criteria.brand),
                ProductSpecifications.minPrice(criteria.minPrice),
                ProductSpecifications.maxPrice(criteria.maxPrice),
                ProductSpecifications.minRating(criteria.minRating),
                ProductSpecifications.inStock(criteria.inStock),
                ProductSpecifications.tag(criteria.tag),
            )
        )

        val result = productRepository.findAll(spec, pageable)
        return PagedResponse(
            products = result.content.map { ProductSummaryResponse.from(it) },
            total = result.totalElements,
            skip = criteria.skip,
            limit = criteria.limit,
        )
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): ProductResponse {
        val product = findOrThrow(id)
        return ProductResponse.from(product)
    }

    @Transactional(readOnly = true)
    fun categories(): List<String> = productRepository.findDistinctCategories()

    /**
     * Csak a 4 szerkeszthető mezőt módosítja: price, discountPercentage, rating, stock.
     * A többi mező változatlan marad.
     */
    @Transactional
    fun update(id: Long, request: ProductUpdateRequest): ProductResponse {
        if (request.isEmpty()) {
            throw IllegalArgumentException("Legalább egy módosítható mezőt meg kell adni: price, discountPercentage, rating, stock")
        }
        val product = findOrThrow(id)

        request.price?.let { product.price = it.normalize(2) }
        request.discountPercentage?.let { product.discountPercentage = it.normalize(2) }
        request.rating?.let { product.rating = it.normalize(2) }
        request.stock?.let { product.stock = it }

        product.updatedAt = OffsetDateTime.now()
        val saved = productRepository.save(product)
        return ProductResponse.from(saved)
    }

    private fun findOrThrow(id: Long): Product =
        productRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Nincs ilyen azonosítójú termék: $id") }

    private fun BigDecimal.normalize(scale: Int): BigDecimal =
        this.setScale(scale, java.math.RoundingMode.HALF_UP)
}

/** A termék-lekérdezés szűrő- és lapozási paraméterei. */
data class ProductQuery(
    val q: String? = null,
    val category: String? = null,
    val brand: String? = null,
    val tag: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val minRating: BigDecimal? = null,
    val inStock: Boolean? = null,
    val sortBy: String = "id",
    val order: String = "asc",
    val limit: Int = 30,
    val skip: Int = 0,
)
