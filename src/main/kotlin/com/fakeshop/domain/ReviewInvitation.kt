package com.fakeshop.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * Egy értékelő-email meghívó. A rendelés sikeres fizetésekor jön létre, és egy
 * háttérfolyamat (ütemező) küldi ki, amikor eljön a [sendAt] időpont.
 *
 * A [token] egy egyedi, kitalálhatatlan azonosító, ami az emailben szereplő
 * linkekben szerepel — ezzel azonosítjuk, melyik rendeléshez tartozik az
 * értékelés, JWT/bejelentkezés nélkül is (mint a Barion PaymentId).
 */
@Entity
@Table(name = "review_invitations")
class ReviewInvitation(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @Column(name = "order_id", nullable = false)
    var orderId: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    /** A címzett e-mail címe (a felhasználó email-je a rendelés idején). */
    @Column(name = "recipient_email", nullable = false, length = 255)
    var recipientEmail: String = "",

    /** Egyedi, kitalálhatatlan token az email-linkekhez. */
    @Column(name = "token", nullable = false, unique = true, length = 64)
    var token: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ReviewInvitationStatus = ReviewInvitationStatus.SCHEDULED,

    /** Ekkor (vagy ez után) kell kiküldeni az emailt. */
    @Column(name = "send_at", nullable = false)
    var sendAt: OffsetDateTime = OffsetDateTime.now(),

    /** Mikor küldtük ki ténylegesen (ha már megtörtént). */
    @Column(name = "sent_at")
    var sentAt: OffsetDateTime? = null,

    /** Utolsó hiba szövege, ha a küldés elbukott (diagnosztikához). */
    @Column(name = "last_error", length = 500)
    var lastError: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
)

enum class ReviewInvitationStatus {
    /** Létrejött, várakozik a küldésre. */
    SCHEDULED,

    /** Sikeresen kiküldve. */
    SENT,

    /** A küldés véglegesen elbukott. */
    FAILED,
}
