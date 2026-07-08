package com.fakeshop.repository

import com.fakeshop.domain.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository :
    JpaRepository<Product, Long>,
    JpaSpecificationExecutor<Product> {

    @Query("select distinct p.category from Product p order by p.category")
    fun findDistinctCategories(): List<String>

    /**
     * Atomi készletcsökkentés: csak akkor von le, ha van elég készlet.
     * Visszaadja az érintett sorok számát (1 = sikerült, 0 = nincs elég készlet).
     */
    @Modifying
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    fun decrementStock(@Param("id") id: Long, @Param("qty") qty: Int): Int

    /** Készlet visszaírása (pl. rendelés lemondásakor). */
    @Modifying
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    fun incrementStock(@Param("id") id: Long, @Param("qty") qty: Int): Int
}
