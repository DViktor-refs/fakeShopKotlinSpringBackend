package com.fakeshop.domain

/**
 * A rendeléshez tartozó Barion fizetés állapota. Ez független az [OrderStatus]-tól:
 * a fizetés csak azt jelzi, hogy a pénz megérkezett-e, a rendelés teljesítését
 * (csomagolás, szállítás) továbbra is az OrderStatus írja le.
 *
 * NOT_STARTED -> PENDING -> SUCCEEDED
 *                        -> FAILED / CANCELED / EXPIRED
 */
enum class PaymentStatus {
    /** Még nem indítottunk fizetést ehhez a rendeléshez. */
    NOT_STARTED,

    /** A Barion Payment/Start megtörtént, a vásárló még nem fejezte be a fizetést. */
    PENDING,

    /** A Barion visszaigazolta, hogy a fizetés sikeresen megtörtént. */
    SUCCEEDED,

    /** A fizetés meghiúsult (pl. elutasított kártya). */
    FAILED,

    /** A vásárló megszakította a fizetést a Barion oldalán. */
    CANCELED,

    /** A fizetési ablak lejárt anélkül, hogy a vásárló befejezte volna. */
    EXPIRED,
}
