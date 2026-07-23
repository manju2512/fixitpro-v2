package com.fixitpro.domain.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {

    Optional<ReviewReply> findByReview_ReviewId(Long reviewId);

    @Query("""
            SELECT r FROM ReviewReply r
            JOIN FETCH r.technician t
            JOIN FETCH t.user
            WHERE r.replyId = :id
            """)
    Optional<ReviewReply> findByIdWithDetails(@Param("id") Long id);
}
