package com.fakeshop.review

import com.fakeshop.domain.ReviewInvitationStatus
import com.fakeshop.repository.OrderRepository
import com.fakeshop.repository.ReviewInvitationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/**
 * Percenként (konfigurálható) megnézi, van-e esedékes értékelő email, és kiküldi.
 * A tényleges késleltetést a [ReviewEmailProperties.delay] adja (ez szabja meg a send_at-et);
 * ez az ütemező csak a küldés végrehajtója.
 */
@Component
class ReviewInvitationScheduler(
    private val invitationRepository: ReviewInvitationRepository,
    private val orderRepository: OrderRepository,
    private val emailSender: EmailSender,
    private val contentBuilder: ReviewEmailContentBuilder,
    private val props: ReviewEmailProperties,
    private val barionPublicBaseUrlProvider: PublicBaseUrlProvider,
) {
    private val log = LoggerFactory.getLogger(ReviewInvitationScheduler::class.java)

    /**
     * Alapból 15 másodpercenként fut, hogy teszteléskor (pl. 10s késleltetés)
     * gyorsan látszódjon az eredmény. Élesben ez a gyakoriság is bőven elég.
     */
    @Scheduled(fixedDelayString = "\${fakeshop.review-email.poll-interval-ms:15000}")
    @Transactional
    fun sendDueInvitations() {
        if (!props.enabled) return

        val due = invitationRepository.findDue(ReviewInvitationStatus.SCHEDULED, OffsetDateTime.now())
        if (due.isEmpty()) return

        log.info("{} esedékes értékelő email kiküldése...", due.size)
        val baseUrl = props.publicBaseUrl.ifBlank { barionPublicBaseUrlProvider.get() }

        for (invitation in due) {
            val order = orderRepository.findById(invitation.orderId).orElse(null)
            if (order == null) {
                invitation.status = ReviewInvitationStatus.FAILED
                invitation.lastError = "A rendelés nem található (id=${invitation.orderId})"
                invitationRepository.save(invitation)
                continue
            }
            try {
                val html = contentBuilder.buildHtml(order, invitation.token, baseUrl)
                emailSender.sendHtml(
                    to = invitation.recipientEmail,
                    subject = "Értékeld a rendelésed – TestStore Shop",
                    html = html,
                )
                invitation.status = ReviewInvitationStatus.SENT
                invitation.sentAt = OffsetDateTime.now()
                invitation.lastError = null
            } catch (e: Exception) {
                log.error("Nem sikerült elküldeni az értékelő emailt (token={}): {}", invitation.token, e.message)
                invitation.status = ReviewInvitationStatus.FAILED
                invitation.lastError = e.message?.take(500)
            }
            invitationRepository.save(invitation)
        }
    }
}
