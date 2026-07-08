package com.fakeshop.review

import com.fakeshop.payment.BarionProperties
import org.springframework.stereotype.Component

/**
 * A backend publikus URL-jét adja vissza az email-linkekhez. Újrahasznosítja a
 * már meglévő PUBLIC_BASE_URL beállítást (amit a Barion is használ), hogy ne kelljen
 * kétszer megadni ugyanazt a Railway változót.
 */
@Component
class PublicBaseUrlProvider(
    private val barionProperties: BarionProperties,
) {
    fun get(): String = barionProperties.publicBaseUrl.trimEnd('/')
}
