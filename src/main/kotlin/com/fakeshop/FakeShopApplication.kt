package com.fakeshop

import com.fakeshop.payment.BarionProperties
import com.fakeshop.review.ReviewEmailProperties
import com.fakeshop.seed.SeedProperties
import com.fakeshop.security.JwtProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(
    SeedProperties::class,
    JwtProperties::class,
    BarionProperties::class,
    ReviewEmailProperties::class,
)
class FakeShopApplication

fun main(args: Array<String>) {
    runApplication<FakeShopApplication>(*args)
}
