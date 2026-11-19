package com.eventsphere.dto;

import com.eventsphere.model.Booking;
import com.eventsphere.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String ticketCode,
        Long eventId,
        String eventTitle,
        LocalDateTime eventStartTime,
        int seats,
        BigDecimal totalAmount,
        BookingStatus status,
        LocalDateTime bookedAt) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getTicketCode(), b.getEvent().getId(),
                b.getEvent().getTitle(), b.getEvent().getStartTime(), b.getSeats(),
                b.getTotalAmount(), b.getStatus(), b.getBookedAt());
    }
}
