package com.fakeshop.review

import com.fakeshop.domain.Order
import com.fakeshop.domain.Review
import com.fakeshop.domain.ReviewInvitation
import com.fakeshop.domain.ReviewInvitationStatus
import com.fakeshop.repository.OrderRepository
import com.fakeshop.repository.ProductRepository
import com.fakeshop.repository.ReviewInvitationRepository
import com.fakeshop.repository.ReviewRepository
import com.fakeshop.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ReviewInvitationService(
    private val invitationRepository: ReviewInvitationRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val reviewRepository: ReviewRepository,
    private val props: ReviewEmailProperties,
) {
    private val log = LoggerFactory.getLogger(ReviewInvitationService::class.java)

    /**
     * A fizetés sikerekor hívjuk: beütemez egy értékelő emailt a konfigurált
     * késleltetéssel. Idempotens – ha már van meghívó ehhez a rendeléshez, nem hoz létre újat.
     */
    @Transactional
    fun scheduleForOrder(order: Order) {
        if (!props.enabled) {
            log.info("Az értékelő email funkció ki van kapcsolva (REVIEW_EMAIL_ENABLED=false), nem ütemezünk (rendelés #{}).", order.id)
            return
        }
        log.info("Értékelő email ütemezése indul a #{} rendeléshez...", order.id)
        val orderId = order.id ?: return

        val user = userRepository.findById(order.userId).orElse(null)
        val email = user?.email?.takeIf { it.isNotBlank() }
        if (email == null) {
            log.warn("Nincs email cím a #{} rendeléshez tartozó felhasználónál, nem ütemezünk értékelő emailt.", orderId)
            return
        }

        val sendAt = OffsetDateTime.now().plus(props.resolvedDelay())
        val invitation = ReviewInvitation(
            orderId = orderId,
            userId = order.userId,
            recipientEmail = email,
            token = UUID.randomUUID().toString().replace("-", ""),
            status = ReviewInvitationStatus.SCHEDULED,
            sendAt = sendAt,
        )
        invitationRepository.save(invitation)
        log.info("Értékelő email beütemezve: rendelés #{}, címzett={}, küldés={}", orderId, email, sendAt)
    }

    /**
     * Egy adott rendelés-tétel adatai a token alapján (az értékelő űrlaphoz).
     * A tétel-adatokat MÉG a tranzakción belül olvassuk ki (a rendelés tételei lusta
     * betöltésűek), hogy a controllerben ne dobjon LazyInitializationException-t.
     */
    @Transactional(readOnly = true)
    fun resolveItem(token: String, productId: Long): ResolvedReviewItem? {
        val invitation = invitationRepository.findByToken(token) ?: return null
        val order = orderRepository.findById(invitation.orderId).orElse(null) ?: return null
        val item = order.items.firstOrNull { it.productId == productId } ?: return null
        return ResolvedReviewItem(
            orderId = order.id ?: 0,
            productId = item.productId,
            productTitle = item.productTitle,
        )
    }

    /**
     * Egy értékelés rögzítése az email-linkből. A [token] azonosítja a rendelést,
     * ezzel ellenőrizzük, hogy a megadott termék tényleg a rendelés része volt-e.
     */
    @Transactional
    fun submitReview(
        token: String,
        productId: Long,
        rating: Int,
        pros: String?,
        cons: String?,
        deliveryRating: Int?,
        wouldRecommend: Boolean?,
    ): Boolean {
        val invitation = invitationRepository.findByToken(token) ?: return false
        val order = orderRepository.findById(invitation.orderId).orElse(null) ?: return false

        // Csak a rendelésben ténylegesen szereplő termékre engedünk értékelést.
        val orderedItem = order.items.firstOrNull { it.productId == productId } ?: return false

        val product = productRepository.findById(productId).orElse(null) ?: return false
        val user = userRepository.findById(invitation.userId).orElse(null)

        val review = Review(
            product = product,
            rating = rating.coerceIn(1, 5),
            comment = null,
            pros = pros?.take(1000)?.takeIf { it.isNotBlank() },
            cons = cons?.take(1000)?.takeIf { it.isNotBlank() },
            deliveryRating = deliveryRating?.coerceIn(1, 5),
            wouldRecommend = wouldRecommend,
            reviewDate = OffsetDateTime.now(),
            reviewerName = user?.username ?: "Vásárló",
            reviewerEmail = invitation.recipientEmail,
        )
        reviewRepository.save(review)
        log.info(
            "Értékelés rögzítve email-linkből: rendelés #{}, termék #{} ({}), rating={}",
            order.id, productId, orderedItem.productTitle, review.rating
        )
        return true
    }
}

/** Az értékelő űrlaphoz szükséges, tranzakción belül kiolvasott tétel-adatok. */
data class ResolvedReviewItem(
    val orderId: Long,
    val productId: Long,
    val productTitle: String,
)
