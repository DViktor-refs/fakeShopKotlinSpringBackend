package com.fakeshop.seed

import com.fakeshop.domain.Dimensions
import com.fakeshop.domain.Product
import com.fakeshop.domain.Review
import com.fakeshop.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime

@ConfigurationProperties(prefix = "fakeshop.seed")
data class SeedProperties(
    /** Be/ki kapcsoló a betöltéshez. */
    val enabled: Boolean = true,
    /** A forrás URL – minden terméket lekér (limit=0). */
    val url: String = "https://dummyjson.com/products?limit=0",
)

/**
 * Induláskor lefut: ha üres a products tábla, betölti a dummyjson adatait.
 * A tényleges importot külön (tranzakciós) bean végzi.
 */
@Component
class DataSeeder(
    private val repository: ProductRepository,
    private val importer: ProductImporter,
    private val props: SeedProperties,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(DataSeeder::class.java)

    override fun run(args: ApplicationArguments?) {
        if (!props.enabled) {
            log.info("Seed kikapcsolva (fakeshop.seed.enabled=false), kihagyom.")
            return
        }
        val existing = repository.count()
        if (existing > 0) {
            log.info("A products tábla nem üres ({} elem), a seedet kihagyom.", existing)
            return
        }
        try {
            val count = importer.importFrom(props.url)
            log.info("Seed kész: {} termék betöltve.", count)
        } catch (ex: Exception) {
            // A betöltés hibája ne akassza meg az indulást – csak naplózzuk.
            log.error("A dummyjson betöltése nem sikerült innen: {}. Hiba: {}", props.url, ex.message, ex)
        }
    }
}

@Service
class ProductImporter(
    private val repository: ProductRepository,
) {
    private val log = LoggerFactory.getLogger(ProductImporter::class.java)

    @Transactional
    fun importFrom(url: String): Int {
        log.info("Termékek betöltése innen: {}", url)
        val client = RestClient.builder().build()
        val payload = client.get()
            .uri(url)
            .retrieve()
            .body(DummyProductList::class.java)

        val source = payload?.products ?: emptyList()
        if (source.isEmpty()) {
            log.warn("A forrás nem adott vissza terméket, nincs mit betölteni.")
            return 0
        }

        val entities = source.map { it.toEntity() }
        repository.saveAll(entities)
        log.info("Betöltve {} termék, összesen {} véleménnyel.",
            entities.size, entities.sumOf { it.reviews.size })
        return entities.size
    }

    private fun DummyProduct.toEntity(): Product {
        val product = Product(
            id = id,
            title = title,
            description = description,
            category = category,
            price = price,
            discountPercentage = discountPercentage,
            rating = rating,
            stock = stock,
            brand = brand,
            sku = sku,
            weight = weight,
            dimensions = Dimensions(
                width = dimensions?.width,
                height = dimensions?.height,
                depth = dimensions?.depth,
            ),
            warrantyInformation = warrantyInformation,
            shippingInformation = shippingInformation,
            availabilityStatus = availabilityStatus,
            returnPolicy = returnPolicy,
            minimumOrderQuantity = minimumOrderQuantity,
            barcode = meta?.barcode,
            qrCode = meta?.qrCode,
            metaCreatedAt = meta?.createdAt,
            metaUpdatedAt = meta?.updatedAt,
            thumbnail = thumbnail,
            tags = tags.toMutableSet(),
            images = images.toMutableList(),
        )
        reviews.forEach { r ->
            product.addReview(
                Review(
                    rating = r.rating,
                    comment = r.comment,
                    reviewDate = r.date ?: OffsetDateTime.now(),
                    reviewerName = r.reviewerName,
                    reviewerEmail = r.reviewerEmail,
                )
            )
        }
        return product
    }
}
