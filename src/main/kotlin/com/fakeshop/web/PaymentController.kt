package com.fakeshop.web

import com.fakeshop.dto.OrderResponse
import com.fakeshop.dto.StartPaymentResponse
import com.fakeshop.payment.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Barion fizetés indítása és a hozzá tartozó nyilvános végpontok
 * (callback a Barion szerverétől, és egy egyszerű visszatérő oldal a böngészőnek).
 */
@RestController
class PaymentController(
    private val paymentService: PaymentService,
) {

    /** Fizetés indítása egy saját rendeléshez. A válasz gatewayUrl-jére kell irányítani a vásárlót. */
    @PostMapping("/api/orders/{id}/pay")
    @ResponseStatus(HttpStatus.CREATED)
    fun pay(authentication: Authentication, @PathVariable id: Long): StartPaymentResponse {
        val result = paymentService.startPayment(authentication.name, id)
        return StartPaymentResponse(
            orderId = result.orderId,
            paymentId = result.paymentId,
            gatewayUrl = result.gatewayUrl,
        )
    }

    /** Kézi állapot-frissítés, ha a kliens nem akar a Barion callback-re várni. */
    @PostMapping("/api/orders/{id}/payment/refresh")
    fun refreshPaymentStatus(authentication: Authentication, @PathVariable id: Long): OrderResponse =
        paymentService.refreshPaymentStatus(authentication.name, id)

    /**
     * A Barion szerver hívja ezt automatikusan, amikor változik egy fizetés állapota.
     * Nyilvános végpont (nincs JWT), a Barion csak a paymentId query paramétert küldi.
     * 200 OK-t kell adnia gyorsan, a tényleges feldolgozás itt szinkron, de gyors (egy API hívás).
     */
    @PostMapping("/api/payments/barion/callback")
    fun barionCallback(@RequestParam(required = false) paymentId: String?): ResponseEntity<Void> {
        if (!paymentId.isNullOrBlank()) {
            paymentService.handleCallback(paymentId)
        }
        return ResponseEntity.ok().build()
    }

    /**
     * Egyszerű, saját visszaigazoló oldal, ahova a Barion a böngészőt irányítja fizetés után
     * (RedirectUrl). Mivel a Barion csak valós http(s) URL-t fogad el RedirectUrl-ként, ez az
     * oldal egy Chrome Custom Tabs-ban nyílik meg az Android appból, majd innen automatikusan
     * továbbdobja a böngészőt az app saját, egyedi séma-linkjére (teststoreshop://barion-return),
     * amit az app AndroidManifest.xml-je elkap, és bezárja a Custom Tabs ablakot.
     */
    @GetMapping("/api/payments/barion/result", produces = [MediaType.TEXT_HTML_VALUE])
    fun barionResult(@RequestParam(required = false) paymentId: String?): String {
        if (paymentId.isNullOrBlank()) {
            return renderResultPage("Ismeretlen fizetés", "Nem érkezett paymentId azonosító.", null)
        }
        val order = paymentService.publicRefreshByPaymentId(paymentId)
        val appReturnUrl = "teststoreshop://barion-return?paymentId=$paymentId"
        return when {
            order == null -> renderResultPage(
                "Ismeretlen fizetés", "Ehhez az azonosítóhoz nem találtunk rendelést.", appReturnUrl
            )
            else -> {
                val (title, message) = when (order.paymentStatus.name) {
                    "SUCCEEDED" -> "Sikeres fizetés" to "Köszönjük! A(z) #${order.id} rendelésed kifizetve, hamarosan feldolgozzuk."
                    "FAILED" -> "Sikertelen fizetés" to "A(z) #${order.id} rendelés fizetése nem sikerült. Próbáld újra az appban."
                    "CANCELED" -> "Megszakított fizetés" to "A(z) #${order.id} rendelés fizetését megszakítottad."
                    "EXPIRED" -> "Lejárt fizetés" to "A(z) #${order.id} rendelés fizetési ablaka lejárt. Próbáld újra az appban."
                    else -> "Fizetés folyamatban" to "A(z) #${order.id} rendelés fizetése még feldolgozás alatt van."
                }
                renderResultPage(title, message, appReturnUrl)
            }
        }
    }

    private fun renderResultPage(title: String, message: String, appReturnUrl: String?): String = """
        <!DOCTYPE html>
        <html lang="hu">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>$title – FakeShop</title>
            ${if (appReturnUrl != null) """<meta http-equiv="refresh" content="1;url=$appReturnUrl">""" else ""}
            <style>
                body { font-family: system-ui, sans-serif; background: #f5f5f5; margin: 0;
                       display: flex; align-items: center; justify-content: center; min-height: 100vh; }
                .card { background: #fff; border-radius: 12px; padding: 32px; max-width: 420px;
                        text-align: center; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
                h1 { font-size: 20px; margin-bottom: 12px; }
                p { color: #555; line-height: 1.5; }
                a.button { display: inline-block; margin-top: 16px; padding: 10px 20px; border-radius: 8px;
                           background: #ff6f00; color: #fff; text-decoration: none; font-weight: 600; }
            </style>
        </head>
        <body>
            <div class="card">
                <h1>$title</h1>
                <p>$message</p>
                ${if (appReturnUrl != null) """
                <p>Egy pillanat, visszairányítunk az alkalmazásba...</p>
                <a class="button" href="$appReturnUrl">Vissza az alkalmazásba</a>
                <script>window.location.href = "$appReturnUrl";</script>
                """ else """<p>Ezt az ablakot most már bezárhatod.</p>"""}
            </div>
        </body>
        </html>
    """.trimIndent()
}
