package com.fixitpro.domain.review;

import com.fixitpro.domain.review.dto.CreateReplyRequest;
import com.fixitpro.domain.review.dto.CreateReviewRequest;
import com.fixitpro.domain.review.dto.ModerateReplyRequest;
import com.fixitpro.domain.review.dto.ReviewResponse;
import com.fixitpro.domain.review.dto.UpdateReviewRequest;
import com.fixitpro.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(principal.getUserId(), request));
    }

    @PutMapping("/api/reviews/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reviewService.update(principal.getUserId(), id, request));
    }

    @GetMapping("/api/reviews/{id}")
    public ResponseEntity<ReviewResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getById(id));
    }

    /** Public: powers a technician's profile/review list. */
    @GetMapping("/api/reviews/technician/{technicianId}")
    public ResponseEntity<List<ReviewResponse>> listForTechnician(@PathVariable Long technicianId) {
        return ResponseEntity.ok(reviewService.listForTechnician(technicianId));
    }

    @GetMapping("/api/reviews/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReviewResponse>> listAllForAdmin() {
        return ResponseEntity.ok(reviewService.listAllForAdmin());
    }

    /** Technician replies once to a review left on one of their own completed jobs. */
    @PostMapping("/api/reviews/{id}/reply")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ReviewResponse> addReply(
            @PathVariable Long id,
            @Valid @RequestBody CreateReplyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.addReply(principal.getUserId(), id, request));
    }

    @PatchMapping("/api/admin/reviews/replies/{replyId}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> moderateReply(
            @PathVariable Long replyId,
            @Valid @RequestBody ModerateReplyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(reviewService.moderateReply(principal.getUserId(), replyId, request.status()));
    }
}
