package com.fixitpro.domain.reservation;

import java.util.Set;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    private static final Set<ReservationStatus> TERMINAL = Set.of(COMPLETED, CANCELLED);

    /**
     * Defines the reservation lifecycle as an explicit graph rather than
     * leaving transition logic scattered across services/controllers.
     * PENDING   -> CONFIRMED, CANCELLED
     * CONFIRMED -> IN_PROGRESS, CANCELLED
     * IN_PROGRESS -> COMPLETED, CANCELLED
     * COMPLETED / CANCELLED -> (terminal, no further transitions)
     */
    public boolean canTransitionTo(ReservationStatus next) {
        if (TERMINAL.contains(this)) {
            return false;
        }
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == COMPLETED || next == CANCELLED;
            default -> false;
        };
    }

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }
}
