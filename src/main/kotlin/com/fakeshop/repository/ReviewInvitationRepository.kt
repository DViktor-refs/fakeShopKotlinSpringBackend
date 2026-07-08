package com.fakeshop.repository

import com.fakeshop.domain.ReviewInvitation
import com.fakeshop.domain.ReviewInvitationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
interface ReviewInvitationRepository : JpaRepository<ReviewInvitation, Long> {

    fun findByToken(token: String): ReviewInvitation?

    /** A most esedékes (SCHEDULED és lejárt send_at) meghívók – ezeket kell kiküldeni. */
    @Query(
        "SELECT ri FROM ReviewInvitation ri " +
            "WHERE ri.status = :status AND ri.sendAt <= :now " +
            "ORDER BY ri.sendAt ASC"
    )
    fun findDue(
        @Param("status") status: ReviewInvitationStatus,
        @Param("now") now: OffsetDateTime,
    ): List<ReviewInvitation>
}
