package com.fakeshop.review

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Az értékelő-email funkció beállításai.
 *
 * Railway / .env változók:
 *  - REVIEW_EMAIL_ENABLED   be/ki kapcsolja a funkciót (alap: true)
 *  - REVIEW_EMAIL_DELAY      mennyivel a fizetés UTÁN menjen az email.
 *                            ISO-8601 duration VAGY egyszerű alak: "10s", "24h", "1d", "PT10S".
 *                            Teszthez pl. 10s, élesben 24h/1d.
 *  - REVIEW_EMAIL_FROM       a feladó email címe (a Resend fiókodban ellenőrzött domain kell legyen,
 *                            vagy a Resend saját teszt-domainje: onboarding@resend.dev)
 *  - RESEND_API_KEY          a Resend API kulcsa (resend.com -> API Keys). Ha üres, nem küld
 *                            valódi emailt, csak logolja a tartalmat (teszteléshez).
 *  - PUBLIC_BASE_URL         (már létező) – ebből épülnek az email-linkek
 */
@ConfigurationProperties(prefix = "fakeshop.review-email")
data class ReviewEmailProperties(
    /** Ki-be kapcsoló. Ha false, nem jön létre és nem megy ki értékelő email. */
    val enabled: Boolean = true,

    /**
     * A fizetés utáni késleltetés a küldésig. Elfogadja az ISO-8601 alakot ("PT24H")
     * és a rövid alakot is ("10s", "30m", "24h", "1d") – lásd [resolvedDelay].
     */
    val delay: String = "24h",

    /** A feladó email címe. A Resendnél ez egy ellenőrzött domain kell legyen. */
    val from: String = "onboarding@resend.dev",

    /** A backend publikus URL-je az email-linkekhez (ha üres, a PUBLIC_BASE_URL-t használjuk). */
    val publicBaseUrl: String = "",

    /** A Resend API kulcsa. Ha üres, az email csak logolva lesz, nem megy ki ténylegesen. */
    val resendApiKey: String = "",
) {
    /**
     * A [delay] szöveget Duration-ná alakítja. Támogatja:
     *  - ISO-8601: "PT24H", "PT10S", "P1D"
     *  - rövid alak: "10s", "30m", "24h", "1d"
     * Hibás érték esetén 24 órára esik vissza.
     */
    fun resolvedDelay(): Duration {
        val raw = delay.trim()
        if (raw.isEmpty()) return Duration.ofHours(24)
        // ISO-8601 alak
        if (raw.startsWith("P", ignoreCase = true) || raw.startsWith("PT", ignoreCase = true)) {
            return runCatching { Duration.parse(raw) }.getOrElse { Duration.ofHours(24) }
        }
        // Rövid alak: szám + egység (s/m/h/d)
        val match = Regex("^(\\d+)\\s*([smhd])$", RegexOption.IGNORE_CASE).find(raw)
            ?: return Duration.ofHours(24)
        val amount = match.groupValues[1].toLong()
        return when (match.groupValues[2].lowercase()) {
            "s" -> Duration.ofSeconds(amount)
            "m" -> Duration.ofMinutes(amount)
            "h" -> Duration.ofHours(amount)
            "d" -> Duration.ofDays(amount)
            else -> Duration.ofHours(24)
        }
    }
}
