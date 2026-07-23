package com.fixitpro.domain.review;

import com.fixitpro.common.exception.DuplicateResourceException;
import com.fixitpro.common.exception.InvalidStateTransitionException;
import com.fixitpro.common.exception.ResourceNotFoundException;
import com.fixitpro.common.exception.UnauthorizedActionException;
import com.fixitpro.domain.reservation.Reservation;
import com.fixitpro.domain.reservation.ReservationRepository;
import com.fixitpro.domain.reservation.ReservationStatus;
import com.fixitpro.domain.review.dto.CreateReplyRequest;
import com.fixitpro.domain.review.dto.CreateReviewRequest;
import com.fixitpro.domain.review.dto.ReviewResponse;
import com.fixitpro.domain.review.dto.UpdateReviewRequest;
import com.fixitpro.domain.technician.TechnicianProfile;
import com.fixitpro.domain.technician.TechnicianService;
import com.fixitpro.domain.user.User;
import com.fixitpro.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final TechnicianService technicianService;

    // --- Reviews (customer) ---

    @Transactional
    public ReviewResponse create(Long customerId, CreateReviewRequest request) {
        Reservation reservation = reservationRepository.findByIdWithDetails(request.reservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + request.reservationId()));

        if (!reservation.getCustomer().getUserId().equals(customerId)) {
            throw new UnauthorizedActionException("You can only review your own reservations");
        }
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new InvalidStateTransitionException("You can only review a completed reservation");
        }
        if (reviewRepository.findByReservation_ReservationId(reservation.getReservationId()).isPresent()) {
            throw new DuplicateResourceException("This reservation already has a review");
        }

        Review review = Review.builder()
                .reservation(reservation)
                .customer(reservation.getCustomer())
                .rating(request.rating())
                .comment(request.comment())
                .build();
        review = reviewRepository.save(review);

        if (reservation.getTechnician() != null) {
            technicianService.recordReviewRating(
                    reservation.getTechnician().getTechnicianId(), null, request.rating());
        }

        return ReviewResponse.from(reviewRepository.findByIdWithDetails(review.getReviewId()).orElseThrow(), null);
    }

    @Transactional
    public ReviewResponse update(Long customerId, Long reviewId, UpdateReviewRequest request) {
        Review review = getEntityOrThrow(reviewId);

        if (!review.getCustomer().getUserId().equals(customerId)) {
            throw new UnauthorizedActionException("You can only edit your own review");
        }

        int oldRating = review.getRating();
        review.setRating(request.rating());
        review.setComment(request.comment());
        review.setEdited(true);

        TechnicianProfile technician = review.getReservation().getTechnician();
        if (technician != null && oldRating != request.rating()) {
            technicianService.recordReviewRating(technician.getTechnicianId(), oldRating, request.rating());
        }

        ReviewReply reply = reviewReplyRepository.findByReview_ReviewId(reviewId).orElse(null);
        return ReviewResponse.from(review, reply);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getById(Long reviewId) {
        Review review = getEntityOrThrow(reviewId);
        ReviewReply reply = reviewReplyRepository.findByReview_ReviewId(reviewId)
                .filter(r -> r.getStatus() != ReviewReplyStatus.DELETED)
                .orElse(null);
        return ReviewResponse.from(review, reply);
    }

    /** Public: reviews for a technician's profile page. Deleted/hidden replies are never shown here. */
    @Transactional(readOnly = true)
    public List<ReviewResponse> listForTechnician(Long technicianId) {
        return reviewRepository.findAllByTechnicianId(technicianId).stream()
                .map(r -> ReviewResponse.from(r, visibleReplyOrNull(r.getReviewId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listAllForAdmin() {
        return reviewRepository.findAllWithDetails().stream()
                .map(r -> ReviewResponse.from(r, reviewReplyRepository.findByReview_ReviewId(r.getReviewId()).orElse(null)))
                .toList();
    }

    // --- Replies (technician) ---

    @Transactional
    public ReviewResponse addReply(Long technicianUserId, Long reviewId, CreateReplyRequest request) {
        Review review = getEntityOrThrow(reviewId);
        TechnicianProfile technician = review.getReservation().getTechnician();

        if (technician == null || !technician.getUser().getUserId().equals(technicianUserId)) {
            throw new UnauthorizedActionException("You can only reply to reviews on your own completed jobs");
        }
        if (reviewReplyRepository.findByReview_ReviewId(reviewId).isPresent()) {
            throw new DuplicateResourceException("You have already replied to this review");
        }

        ReviewReply reply = ReviewReply.builder()
                .review(review)
                .technician(technician)
                .replyText(request.replyText())
                .status(ReviewReplyStatus.VISIBLE)
                .build();
        reviewReplyRepository.save(reply);

        return ReviewResponse.from(review, reply);
    }

    // --- Moderation (admin) ---

    @Transactional
    public ReviewResponse moderateReply(Long adminUserId, Long replyId, String statusRaw) {
        ReviewReply reply = reviewReplyRepository.findByIdWithDetails(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found: " + replyId));

        ReviewReplyStatus newStatus = parseStatus(statusRaw);
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminUserId));

        reply.setStatus(newStatus);
        reply.setModeratedBy(admin);

        Review review = getEntityOrThrow(reply.getReview().getReviewId());
        return ReviewResponse.from(review, reply);
    }

    // --- internal helpers ---

    private Review getEntityOrThrow(Long id) {
        return reviewRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
    }

    private ReviewReply visibleReplyOrNull(Long reviewId) {
        Optional<ReviewReply> reply = reviewReplyRepository.findByReview_ReviewId(reviewId);
        return reply.filter(r -> r.getStatus() == ReviewReplyStatus.VISIBLE).orElse(null);
    }

    private ReviewReplyStatus parseStatus(String raw) {
        try {
            return ReviewReplyStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidStateTransitionException("Unknown reply status: " + raw);
        }
    }
}
