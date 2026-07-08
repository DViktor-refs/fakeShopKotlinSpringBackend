package com.fakeshop.web

import com.fakeshop.dto.PagedResponse
import com.fakeshop.dto.ProductResponse
import com.fakeshop.dto.ProductSummaryResponse
import com.fakeshop.dto.ProductUpdateRequest
import com.fakeshop.service.ProductQuery
import com.fakeshop.service.ProductService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
) {

    /**
     * Termékek listázása szűréssel, kereséssel, rendezéssel és lapozással.
     *
     * Példák:
     *  GET /api/products
     *  GET /api/products?q=lipstick
     *  GET /api/products?category=beauty&minRating=4&sortBy=price&order=desc
     *  GET /api/products?minPrice=10&maxPrice=100&inStock=true&limit=10&skip=20
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) brand: String?,
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) minRating: BigDecimal?,
        @RequestParam(required = false) inStock: Boolean?,
        @RequestParam(defaultValue = "id") sortBy: String,
        @RequestParam(defaultValue = "asc") order: String,
        @RequestParam(defaultValue = "30") limit: Int,
        @RequestParam(defaultValue = "0") skip: Int,
    ): PagedResponse<ProductSummaryResponse> {
        val query = ProductQuery(
            q = q,
            category = category,
            brand = brand,
            tag = tag,
            minPrice = minPrice,
            maxPrice = maxPrice,
            minRating = minRating,
            inStock = inStock,
            sortBy = sortBy,
            order = order,
            limit = limit.coerceIn(1, 100),
            skip = skip.coerceAtLeast(0),
        )
        return productService.search(query)
    }

    /** Az összes létező kategória. */
    @GetMapping("/categories")
    fun categories(): List<String> = productService.categories()

    /** Egy termék teljes adatai azonosító alapján. */
    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): ProductResponse = productService.getById(id)

    /**
     * Termék módosítása. Csak a price, discountPercentage, rating és stock írható.
     * PATCH és PUT is ugyanazt csinálja (részleges frissítés).
     */
    @PatchMapping("/{id}")
    fun patch(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductUpdateRequest,
    ): ProductResponse = productService.update(id, request)

    @PutMapping("/{id}")
    fun put(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductUpdateRequest,
    ): ProductResponse = productService.update(id, request)
}
