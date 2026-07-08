package com.fakeshop.payment

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Barion Smart Gateway beállítások.
 *
 * Railway-en (vagy helyi .env-ben) állítandó változók:
 *  - POSKEY            a Barion POS titkos kulcsa (kötelező)
 *  - PAYEE              a pénzt fogadó Barion-fiók e-mail címe (kötelező)
 *  - BARION_BASE_URL    api.test.barion.com (sandbox, alapértelmezett) vagy api.barion.com (éles)
 *  - BARION_CURRENCY    HUF / EUR / USD – a Barion POS fióknak támogatnia kell
 *  - PUBLIC_BASE_URL    a backend publikusan elérhető URL-je (pl. Railway domain),
 *                       ebből épül fel a Barion callback URL-je
 *  - BARION_REDIRECT_URL opcionális: ha van külön frontend "köszönjük" oldal, ide mutasson;
 *                       alapból a backend saját, egyszerű visszaigazoló oldalát használja
 */
@ConfigurationProperties(prefix = "fakeshop.barion")
data class BarionProperties(
    /** A Barion POS titkos kulcsa. ÉLESBEN soha ne kerüljön kódba, csak env változóba! */
    val posKey: String = "",

    /** A pénzt fogadó Barion-fiók e-mail címe (a te webshopod Barion accountja). */
    val payee: String = "",

    /** Sandbox alapértelmezett; éles környezetben BARION_BASE_URL=https://api.barion.com */
    val baseUrl: String = "https://api.test.barion.com",

    /** A fizetés pénzneme. A Barion POS fióknak támogatnia kell. */
    val currency: String = "HUF",

    /**
     * A termékárak (dummyjson) dollárban vannak eltárolva, de a HUF-os fizetéshez át kell váltani.
     * Ennek a szorzónak PONTOSAN egyeznie kell az Android app megjelenítési átváltásával
     * (lásd StoreTestWithPayment: ui/util/PriceFormatter.kt, USD_TO_HUF), különben a Barionnak
     * elküldött összeg nem egyezik azzal, amit a vásárló az appban látott.
     */
    val usdToHufRate: Double = 300.0,

    /** A backend publikus, kívülről elérhető alap URL-je (Railway domain), séma nélkül-tel is jó, de https ajánlott. */
    val publicBaseUrl: String = "http://localhost:8080",

    /**
     * Hova irányítsa vissza a böngészőt fizetés után. Ha üresen hagyod,
     * a backend saját, beépített visszaigazoló oldalát használja
     * ({publicBaseUrl}/api/payments/barion/result).
     */
    val redirectUrl: String = "",
) {
    /** A Barion ide küldi a szerver-szerver callback-et, amikor változik a fizetés állapota. */
    fun resolvedCallbackUrl(): String = "$publicBaseUrl/api/payments/barion/callback"

    /** A böngésző ide tér vissza a fizetés után; ha nincs külön frontend, a saját oldalunkra. */
    fun resolvedRedirectUrl(): String =
        redirectUrl.ifBlank { "$publicBaseUrl/api/payments/barion/result" }
}
