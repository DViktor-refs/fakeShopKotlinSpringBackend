package com.fakeshop.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

/**
 * Vékony réteg a Barion Smart Gateway v2 Payment API fölött.
 * Csak azt a két hívást tudja, amire itt szükség van: fizetés indítása és lekérdezése.
 */
@Component
class BarionClient(
    private val props: BarionProperties,
) {
    private val log = LoggerFactory.getLogger(BarionClient::class.java)

    private val client: RestClient by lazy {
        RestClient.builder().baseUrl(props.baseUrl).build()
    }

    /**
     * Elindít egy fizetést a Barionnál. A visszaadott [BarionStartPaymentResponse]
     * `errors` mezője akkor is ki lehet töltve, ha a HTTP hívás technikailag sikeres volt
     * (pl. rossz POSKey vagy nem támogatott pénznem) – ezt a hívónak ellenőriznie kell.
     */
    fun startPayment(request: BarionStartPaymentRequest): BarionStartPaymentResponse {
        log.info("Barion fizetés indítása, PaymentRequestId={}", request.paymentRequestId)
        val response = try {
            client.post()
                .uri("/v2/Payment/Start")
                .body(request)
                .retrieve()
                .body<BarionStartPaymentResponse>()
                ?: throw BarionApiException("A Barion nem adott vissza választ a Payment/Start hívásra")
        } catch (ex: RestClientResponseException) {
            throw toBarionApiException(ex)
        }

        if (response.errors.isNotEmpty()) {
            val details = response.errors.joinToString("; ") { "${it.errorCode}: ${it.description ?: it.title}" }
            log.error("Barion Payment/Start hiba: {}", details)
            throw BarionApiException("A Barion elutasította a fizetés indítását: $details")
        }
        return response
    }

    /**
     * Lekérdezi egy fizetés aktuális állapotát. Ezt MINDIG meg kell hívni a callback
     * után (vagy amikor a vásárló visszatér) – a callback maga csak egy jelzés,
     * önmagában nem bizonyítja a fizetés sikerességét.
     */
    fun getPaymentState(paymentId: String): BarionPaymentStateResponse {
        log.info("Barion fizetési állapot lekérdezése, PaymentId={}", paymentId)
        return try {
            client.get()
                .uri { builder ->
                    builder.path("/v2/Payment/GetPaymentState")
                        .queryParam("POSKey", props.posKey)
                        .queryParam("PaymentId", paymentId)
                        .build()
                }
                .retrieve()
                .body<BarionPaymentStateResponse>()
                ?: throw BarionApiException("A Barion nem adott vissza választ a GetPaymentState hívásra")
        } catch (ex: RestClientResponseException) {
            throw toBarionApiException(ex)
        }
    }

    /**
     * A Barion hibaválaszai (4xx/5xx) JSON body-ban jönnek, {"Errors":[{"Description":"..."}]}
     * formában. Ezt próbáljuk kiolvasni, hogy értelmes hibaüzenetet adjunk vissza –
     * ha ez nem sikerül, legalább a nyers választ mutatjuk.
     */
    private fun toBarionApiException(ex: RestClientResponseException): BarionApiException {
        val rawBody = ex.responseBodyAsString
        log.error("Barion API hiba ({}): {}", ex.statusCode, rawBody)
        val parsedMessage = try {
            val errors = com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(rawBody, BarionStartPaymentResponse::class.java)
                .errors
            errors.joinToString("; ") { "${it.errorCode}: ${it.description ?: it.title}" }.ifBlank { null }
        } catch (parseEx: Exception) {
            null
        }
        return BarionApiException(
            parsedMessage
                ?: "A Barion HTTP ${ex.statusCode.value()} hibát adott vissza: ${rawBody.take(500)}"
        )
    }
}

class BarionApiException(message: String) : RuntimeException(message)
