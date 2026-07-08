package com.fakeshop.payment

import com.fakeshop.domain.Order
import com.fakeshop.domain.OrderStatus
import com.fakeshop.domain.PaymentStatus
import com.fakeshop.dto.OrderResponse
import com.fakeshop.repository.OrderRepository
import com.fakeshop.repository.ProductRepository
import com.fakeshop.service.CurrentUserService
import com.fakeshop.web.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID

data class StartPaymentResult(
    val orderId: Long,
    val paymentId: String,
    val gatewayUrl: String,
)

@Service
class PaymentService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val currentUserService: CurrentUserService,
    private val barionClient: BarionClient,
    private val props: BarionProperties,
    private val reviewInvitationService: com.fakeshop.review.ReviewInvitationService,
) {
    private val log = LoggerFactory.getLogger(PaymentService::class.java)

    /**
     * A bejelentkezett felhasználó saját, még ki nem fizetett rendeléséhez
     * indít egy Barion fizetést, és visszaadja a GatewayUrl-t, ahova a
     * kliensnek (böngésző / Android WebView) irányítania kell a vásárlót.
     */
    @Transactional
    fun startPayment(username: String, orderId: Long): StartPaymentResult {
        val user = currentUserService.require(username)
        val order = orderRepository.findByIdAndUserId(orderId, user.id!!)
            ?: throw ResourceNotFoundException("Nincs ilyen rendelés: $orderId")

        require(props.posKey.isNotBlank()) { "A Barion POSKEY nincs beállítva a szerveren" }
        require(props.payee.isNotBlank()) { "A Barion PAYEE (fogadó e-mail) nincs beállítva a szerveren" }

        if (order.status == OrderStatus.CANCELLED) {
            throw IllegalArgumentException("Ez a rendelés le van mondva, nem fizethető ki")
        }
        if (order.paymentStatus == PaymentStatus.SUCCEEDED) {
            throw IllegalArgumentException("Ez a rendelés már ki van fizetve")
        }
        if (order.paymentStatus == PaymentStatus.PENDING) {
            throw IllegalArgumentException(
                "Már folyamatban van egy fizetés ehhez a rendeléshez. Ha megszakadt, várd meg a lejáratát, " +
                    "vagy kérdezd le az állapotát a /api/orders/${order.id}/payment/refresh végponton."
            )
        }

        val paymentRequestId = "fakeshop-order-${order.id}-${UUID.randomUUID().toString().take(8)}"

        // A Barion pénznemenként eltérő tizedesjegy-pontosságot vár el
        // (pl. a HUF-nak nincs tizedesjegye) – ehhez igazítjuk a küldött összegeket.
        // FONTOS: a tárolt árak dollárban vannak, ezért HUF fizetésnél át is váltjuk őket,
        // ugyanazzal a szorzóval, amit az Android app is használ megjelenítéskor —
        // különben a Barionnak elküldött összeg nem egyezne azzal, amit a vásárló látott.
        val scale = currencyScale(props.currency)
        val roundedItems = order.items.map { item ->
            val convertedUnitPrice = convertToChargeCurrency(item.unitPrice)
            val unitPrice = convertedUnitPrice.setScale(scale, RoundingMode.HALF_UP)
            val itemTotal = unitPrice.multiply(BigDecimal(item.quantity)).setScale(scale, RoundingMode.HALF_UP)
            BarionItem(
                name = item.productTitle.take(250),
                description = item.productTitle.take(500),
                quantity = item.quantity,
                unit = "db",
                unitPrice = unitPrice,
                itemTotal = itemTotal,
                sku = item.productId.toString(),
            )
        }
        val roundedTotal = roundedItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.itemTotal) }

        val transaction = BarionTransaction(
            posTransactionId = "order-${order.id}",
            payee = props.payee,
            total = roundedTotal,
            comment = "FakeShop rendelés #${order.id}",
            items = roundedItems,
        )

        val request = BarionStartPaymentRequest(
            posKey = props.posKey,
            paymentRequestId = paymentRequestId,
            redirectUrl = props.resolvedRedirectUrl(),
            callbackUrl = props.resolvedCallbackUrl(),
            transactions = listOf(transaction),
            currency = props.currency,
            orderNumber = order.id.toString(),
        )

        val response = barionClient.startPayment(request)
        val paymentId = response.paymentId
            ?: throw BarionApiException("A Barion nem adott vissza PaymentId-t")

        order.paymentId = paymentId
        order.paymentRequestId = paymentRequestId
        order.paymentStatus = PaymentStatus.PENDING
        order.barionStatus = response.status
        order.updatedAt = OffsetDateTime.now()
        orderRepository.save(order)

        return StartPaymentResult(
            orderId = order.id!!,
            paymentId = paymentId,
            gatewayUrl = response.gatewayUrl
                ?: throw BarionApiException("A Barion nem adott vissza GatewayUrl-t"),
        )
    }

    /**
     * A Barion szerver-szerver callback-je hívja ezt PaymentId alapján.
     * A callback önmagában csak jelzés, ezért itt mindig lekérdezzük a
     * tényleges állapotot a Barion API-tól, mielőtt bármit is módosítanánk.
     */
    @Transactional
    fun handleCallback(paymentId: String) {
        val order = orderRepository.findByPaymentId(paymentId)
        if (order == null) {
            log.warn("Barion callback ismeretlen PaymentId-vel érkezett: {}", paymentId)
            return
        }
        refreshFromBarion(order)
    }

    /**
     * Kézi állapot-frissítés: a kliens (pl. az Android app, miután a vásárló
     * visszatért a Barion oldaláról) ezt hívhatja, ha nem akar a callback-re várni.
     *
     * FONTOS: a DTO-vá alakítást ITT, a tranzakción belül végezzük el (nem a controllerben!),
     * mert az Order.items lusta (LAZY) betöltésű – a tranzakció lezárása után már nem
     * lenne elérhető, és LazyInitializationException-t dobna.
     */
    @Transactional
    fun refreshPaymentStatus(username: String, orderId: Long): OrderResponse {
        val user = currentUserService.require(username)
        val order = orderRepository.findByIdAndUserId(orderId, user.id!!)
            ?: throw ResourceNotFoundException("Nincs ilyen rendelés: $orderId")

        val refreshed = if (order.paymentId == null) order else refreshFromBarion(order)
        return OrderResponse.from(refreshed)
    }

    /**
     * A nyilvános visszatérő oldalhoz: a Barion PaymentId önmagában is
     * elég "titkos" (Barion generálja, nem kitalálható), ezért ez a metódus
     * JWT nélkül is meghívható, csak a paymentId birtokában.
     */
    @Transactional
    fun publicRefreshByPaymentId(paymentId: String): Order? {
        val order = orderRepository.findByPaymentId(paymentId) ?: return null
        return refreshFromBarion(order)
    }

    private val terminalStatuses = setOf(
        PaymentStatus.SUCCEEDED, PaymentStatus.FAILED, PaymentStatus.CANCELED, PaymentStatus.EXPIRED
    )

    private fun refreshFromBarion(order: Order): Order {
        val paymentId = order.paymentId ?: return order

        // Ha a fizetés már véglegesen eldőlt, felesleges (és a Barion sandbox rate limitje miatt
        // kockázatos) újra lekérdezni — ilyenkor egyszerűen a mentett állapotot adjuk vissza.
        // Ez azért fontos, mert a fizetés utáni visszatéréskor TÖBB HELYRŐL is jöhet lekérdezés
        // szinte egyszerre (a saját HTML visszaigazoló oldalunkról ÉS az app saját hívásából is),
        // és ezek összeadva könnyen belefutnak a Barion 429-es rate limitjébe.
        if (order.paymentStatus in terminalStatuses) {
            return order
        }

        val state = barionClient.getPaymentState(paymentId)
        val newStatus = mapStatus(state.status)

        if (newStatus == order.paymentStatus && state.status == order.barionStatus) {
            return order
        }

        log.info("Rendelés #{} fizetési állapota: {} -> {} (Barion: {})",
            order.id, order.paymentStatus, newStatus, state.status)

        order.barionStatus = state.status
        order.paymentStatus = newStatus
        order.updatedAt = OffsetDateTime.now()

        when (newStatus) {
            PaymentStatus.SUCCEEDED -> {
                if (order.status == OrderStatus.PENDING) {
                    order.status = OrderStatus.CONFIRMED
                }
                // Sikeres fizetés → beütemezzük az értékelő emailt (konfigurálható késleltetéssel).
                // Ezt szándékosan külön hibakezeléssel védjük: az email-funkció semmilyen hibája
                // nem görgetheti vissza a fizetési állapot mentését.
                try {
                    reviewInvitationService.scheduleForOrder(order)
                } catch (e: Exception) {
                    log.error("Nem sikerült beütemezni az értékelő emailt a #{} rendeléshez: {}", order.id, e.message, e)
                }
            }
            PaymentStatus.FAILED, PaymentStatus.CANCELED, PaymentStatus.EXPIRED -> {
                if (order.status == OrderStatus.PENDING) {
                    restoreStock(order)
                    order.status = OrderStatus.CANCELLED
                }
            }
            else -> Unit
        }

        return orderRepository.save(order)
    }

    private fun restoreStock(order: Order) {
        for (item in order.items) {
            productRepository.incrementStock(item.productId, item.quantity)
        }
    }

    private fun mapStatus(barionStatus: String?): PaymentStatus = when (barionStatus) {
        "Succeeded", "PartiallySucceeded" -> PaymentStatus.SUCCEEDED
        "Failed" -> PaymentStatus.FAILED
        "Canceled" -> PaymentStatus.CANCELED
        "Expired" -> PaymentStatus.EXPIRED
        "Prepared", "Started", "InProgress", "Reserved", "Authorized" -> PaymentStatus.PENDING
        else -> {
            log.warn("Ismeretlen Barion állapot: {}", barionStatus)
            PaymentStatus.PENDING
        }
    }

    /** Hány tizedesjegyet fogad el a Barion az adott pénznemnél. */
    private fun currencyScale(currency: String): Int = when (currency.uppercase()) {
        "HUF" -> 0
        else -> 2
    }

    /**
     * A tárolt árak dollárban vannak. HUF fizetésnél átváltjuk őket a konfigurált árfolyamon
     * (ugyanazzal, amit az Android app is használ megjelenítéskor). Más pénznemnél
     * (pl. USD) nincs átváltás — ott feltételezzük, hogy az ár már a megfelelő egységben van.
     */
    private fun convertToChargeCurrency(usdAmount: BigDecimal): BigDecimal =
        when (props.currency.uppercase()) {
            "HUF" -> usdAmount.multiply(BigDecimal(props.usdToHufRate))
            else -> usdAmount
        }
}
