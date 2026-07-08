package com.fakeshop.repository

import com.fakeshop.domain.Product
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal

/**
 * Összerakható szűrőfeltételek a termékek lekérdezéséhez.
 * Minden feltétel opcionális; a null értékűek kimaradnak.
 */
object ProductSpecifications {

    fun search(q: String?): Specification<Product>? {
        if (q.isNullOrBlank()) return null
        val like = "%${q.trim().lowercase()}%"
        return Specification { root, _, cb ->
            cb.or(
                cb.like(cb.lower(root.get<String>("title")), like),
                cb.like(cb.lower(root.get<String>("description")), like),
                cb.like(cb.lower(root.get<String>("brand")), like),
            )
        }
    }

    fun category(category: String?): Specification<Product>? {
        if (category.isNullOrBlank()) return null
        return Specification { root, _, cb ->
            cb.equal(cb.lower(root.get<String>("category")), category.trim().lowercase())
        }
    }

    fun brand(brand: String?): Specification<Product>? {
        if (brand.isNullOrBlank()) return null
        return Specification { root, _, cb ->
            cb.equal(cb.lower(root.get<String>("brand")), brand.trim().lowercase())
        }
    }

    fun minPrice(min: BigDecimal?): Specification<Product>? {
        if (min == null) return null
        return Specification { root, _, cb ->
            cb.greaterThanOrEqualTo(root.get<BigDecimal>("price"), min)
        }
    }

    fun maxPrice(max: BigDecimal?): Specification<Product>? {
        if (max == null) return null
        return Specification { root, _, cb ->
            cb.lessThanOrEqualTo(root.get<BigDecimal>("price"), max)
        }
    }

    fun minRating(min: BigDecimal?): Specification<Product>? {
        if (min == null) return null
        return Specification { root, _, cb ->
            cb.greaterThanOrEqualTo(root.get<BigDecimal>("rating"), min)
        }
    }

    fun inStock(inStock: Boolean?): Specification<Product>? {
        if (inStock == null) return null
        return Specification { root, _, cb ->
            if (inStock) cb.greaterThan(root.get<Int>("stock"), 0)
            else cb.equal(root.get<Int>("stock"), 0)
        }
    }

    fun tag(tag: String?): Specification<Product>? {
        if (tag.isNullOrBlank()) return null
        return Specification { root, query, cb ->
            // distinct, hogy a tag-join ne duplikálja a termékeket
            query?.distinct(true)
            val tags = root.join<Product, String>("tags")
            cb.equal(cb.lower(tags), tag.trim().lowercase())
        }
    }

    /** Több feltétel egyetlen Specification-né fűzése (a null-okat kihagyva). */
    fun combine(specs: List<Specification<Product>?>): Specification<Product>? {
        val active = specs.filterNotNull()
        if (active.isEmpty()) return null
        return active.reduce { acc, spec -> acc.and(spec) }
    }
}
