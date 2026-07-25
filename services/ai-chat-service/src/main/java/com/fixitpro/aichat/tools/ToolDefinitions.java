package com.fixitpro.aichat.tools;

import com.fixitpro.aichat.llm.ToolSpec;

import java.util.List;
import java.util.Map;

/**
 * The booking actions the assistant can take. Kept intentionally small and
 * customer-focused (this is a booking assistant, not a general admin
 * console) - core-service still enforces role permissions on every call
 * regardless, so a technician/admin using the chat can't do anything their
 * own account couldn't already do via the normal UI.
 *
 * Time slots aren't a core-service concept yet (no endpoint for them), so
 * this mirrors the same fixed list the frontend's booking form uses -
 * see frontend/src/constants.ts TIME_SLOTS. Keep these in sync.
 */
public final class ToolDefinitions {

    private ToolDefinitions() {
    }

    public static final List<String> VALID_TIME_SLOTS =
            List.of("09:00-11:00", "11:00-13:00", "13:00-15:00", "15:00-17:00", "17:00-19:00");

    public static List<ToolSpec> all() {
        return List.of(
                ToolSpec.function(
                        "list_service_types",
                        "List the kinds of home-repair services FixitPro offers (e.g. Electrician, Plumber, Carpenter), with descriptions and starting prices. Call this whenever you need to know what services exist or their IDs.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(),
                                "required", List.of()
                        )
                ),
                ToolSpec.function(
                        "list_technicians",
                        "List technicians available for a given service type, with their experience and rating. Use this to help the customer pick a technician, or to confirm availability before booking.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "serviceTypeId", Map.of(
                                                "type", "integer",
                                                "description", "The serviceTypeId from list_service_types."
                                        )
                                ),
                                "required", List.of("serviceTypeId")
                        )
                ),
                ToolSpec.function(
                        "create_reservation",
                        "Book a repair appointment for the customer. Only call this once you have all required details confirmed with the customer - don't guess an address, phone number, or date. "
                                + "Valid timeSlot values are exactly: " + String.join(", ", VALID_TIME_SLOTS) + ". "
                                + "reservationDate must be in the future (today or later), formatted YYYY-MM-DD. "
                                + "technicianId is optional - if omitted, the system automatically assigns the best available technician for that service type.",
                        Map.of(
                                "type", "object",
                                "properties", Map.ofEntries(
                                        Map.entry("serviceTypeId", Map.of("type", "integer", "description", "From list_service_types.")),
                                        Map.entry("technicianId", Map.of("type", "integer", "description", "Optional - from list_technicians. Omit to auto-assign.")),
                                        Map.entry("reservationDate", Map.of("type", "string", "description", "YYYY-MM-DD, today or later.")),
                                        Map.entry("timeSlot", Map.of("type", "string", "description", "One of: " + String.join(", ", VALID_TIME_SLOTS))),
                                        Map.entry("address", Map.of("type", "string", "description", "Full service address.")),
                                        Map.entry("telephone", Map.of("type", "string", "description", "Contact phone number, 7-15 digits, optional leading +.")),
                                        Map.entry("comments", Map.of("type", "string", "description", "Optional notes about the problem."))
                                ),
                                "required", List.of("serviceTypeId", "reservationDate", "timeSlot", "address", "telephone")
                        )
                ),
                ToolSpec.function(
                        "list_my_reservations",
                        "List the current customer's own bookings, with their status (PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, or CANCELLED). Call this before cancelling anything, or when the customer asks about their bookings.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(),
                                "required", List.of()
                        )
                ),
                ToolSpec.function(
                        "cancel_reservation",
                        "Cancel one of the customer's own bookings. Only PENDING, CONFIRMED, or IN_PROGRESS bookings can be cancelled - check list_my_reservations first if you're not sure of the reservationId or current status.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "reservationId", Map.of("type", "integer", "description", "From list_my_reservations.")
                                ),
                                "required", List.of("reservationId")
                        )
                )
        );
    }
}
