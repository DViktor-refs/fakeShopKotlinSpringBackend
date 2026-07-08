package com.fakeshop.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.math.BigDecimal

/**
 * A termék fizikai méretei (a dummyjson "dimensions" objektuma).
 * Beágyazott érték, a products táblában tárolódik (dim_width, dim_height, dim_depth).
 */
@Embeddable
class Dimensions(
    @Column(name = "dim_width")
    var width: BigDecimal? = null,

    @Column(name = "dim_height")
    var height: BigDecimal? = null,

    @Column(name = "dim_depth")
    var depth: BigDecimal? = null,
)
