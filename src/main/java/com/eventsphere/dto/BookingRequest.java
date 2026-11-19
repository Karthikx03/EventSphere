package com.eventsphere.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** JSON body for POST /api/bookings. */
public record BookingRequest(
        @NotNull(message = "eventId is required") Long eventId,
        @Min(value = 1, message = "Book at least 1 seat")
        @Max(value = 10, message = "You can book at most 10 seats at once") int seats) {
}
