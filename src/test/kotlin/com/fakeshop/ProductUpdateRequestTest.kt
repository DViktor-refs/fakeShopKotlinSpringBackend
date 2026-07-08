package com.fakeshop

import com.fakeshop.dto.ProductUpdateRequest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductUpdateRequestTest {

    @Test
    fun `ures keres detektalasa`() {
        assertTrue(ProductUpdateRequest().isEmpty())
    }

    @Test
    fun `nem ures, ha legalabb egy mezo ki van toltve`() {
        assertFalse(ProductUpdateRequest(price = BigDecimal("9.99")).isEmpty())
        assertFalse(ProductUpdateRequest(stock = 5).isEmpty())
    }
}
