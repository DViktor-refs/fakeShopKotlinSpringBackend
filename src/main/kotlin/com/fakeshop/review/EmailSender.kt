package com.fakeshop.review

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

/**
 * HTML email küldése a Resend HTTP API-n keresztül (https://resend.com).
 *
 * Miért HTTP API és nem SMTP: a legtöbb felhő-hosztolás (pl. Railway Free/Hobby csomagja)
 * teljesen blokkolja a kimenő SMTP portokat (25/465/587) visszaélés-védelem miatt, így egy
 * sima SMTP-kapcsolat sose jönne létre (örökké timeout-olna). A Resend (és a hasonló
 * tranzakciós email szolgáltatók) sima HTTPS-en keresztül működnek, ez mindenhol megy.
 *
 * Ha a RESEND_API_KEY nincs beállítva, nem küld valódi emailt, csak logolja a tartalmat –
 * így API-kulcs nélkül is tesztelhető a funkció (a linket kimásolva a logból).
 */
@Component
class EmailSender(
    private val props: ReviewEmailProperties,
) {
    private val log = LoggerFactory.getLogger(EmailSender::class.java)

    private val client: RestClient by lazy {
        RestClient.builder().baseUrl("https://api.resend.com").build()
    }

    fun sendHtml(to: String, subject: String, html: String) {
        if (props.resendApiKey.isBlank()) {
            log.warn(
                "RESEND_API_KEY nincs beállítva. Az email NEM lett elküldve, csak logolva. Címzett={}, tárgy={}",
                to, subject
            )
            log.info("--- ÉRTÉKELŐ EMAIL (nem küldve, csak log) ---\nTo: {}\nSubject: {}\n{}", to, subject, html)
            return
        }

        try {
            val response = client.post()
                .uri("/emails")
                .header("Authorization", "Bearer ${props.resendApiKey}")
                .body(ResendEmailRequest(from = props.from, to = listOf(to), subject = subject, html = html))
                .retrieve()
                .body<ResendEmailResponse>()

            log.info("Értékelő email elküldve Resenden keresztül: címzett={}, id={}", to, response?.id)
        } catch (ex: RestClientResponseException) {
            log.error(
                "A Resend API hibát adott vissza (HTTP {}): {}",
                ex.statusCode, ex.responseBodyAsString
            )
            throw EmailSendException("A Resend elutasította az email küldését: ${ex.responseBodyAsString}")
        }
    }
}

class EmailSendException(message: String) : RuntimeException(message)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ResendEmailRequest(
    @JsonProperty("from") val from: String,
    @JsonProperty("to") val to: List<String>,
    @JsonProperty("subject") val subject: String,
    @JsonProperty("html") val html: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ResendEmailResponse(
    @JsonProperty("id") val id: String? = null,
)
