package com.fakeshop.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class InfoController {

    @GetMapping("/")
    fun root(): Map<String, Any> = mapOf(
        "name" to "FakeShop Kotlin Spring Backend",
        "status" to "ok",
        "endpoints" to mapOf(
            "auth" to listOf(
                "POST   /api/auth/login          (username + password -> JWT token)",
                "GET    /api/auth/me             (token)",
            ),
            "products" to listOf(
                "GET    /api/products",
                "GET    /api/products/{id}",
                "GET    /api/products/categories",
                "PATCH  /api/products/{id}       (token; price, discountPercentage, rating, stock)",
                "PUT    /api/products/{id}        (token)",
                "GET    /api/products/{id}/reviews",
                "POST   /api/products/{id}/reviews  (token)",
            ),
            "cart" to listOf(
                "GET    /api/cart                (token)",
                "POST   /api/cart/items          (token; { productId, quantity })",
                "PUT    /api/cart/items/{productId}  (token; { quantity })",
                "DELETE /api/cart/items/{productId}  (token)",
                "DELETE /api/cart                (token)",
            ),
            "orders" to listOf(
                "POST   /api/orders/checkout     (token; kosárból rendelés + készletlevonás)",
                "GET    /api/orders              (token; saját rendelések)",
                "GET    /api/orders/{id}         (token)",
                "POST   /api/orders/{id}/cancel  (token; készlet visszaírás)",
                "PATCH  /api/orders/{id}/status  (ADMIN; { status })",
            ),
            "payments" to listOf(
                "POST   /api/orders/{id}/pay             (token; Barion fizetés indítása -> gatewayUrl)",
                "POST   /api/orders/{id}/payment/refresh (token; állapot lekérdezése a Barionnál)",
                "POST   /api/payments/barion/callback    (nyilvános; Barion szerver hívja)",
                "GET    /api/payments/barion/result      (nyilvános; böngésző visszatérő oldala)",
            ),
            "health" to listOf("GET /actuator/health"),
            "review-email" to listOf(
                "GET  /review/{token}/rate?productId=..&rating=N  (nyilvános; email csillag-link -> űrlap)",
                "POST /review/{token}/submit                     (nyilvános; értékelés beküldése)",
            ),
        ),
    )
}
