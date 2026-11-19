package com.eventsphere.dto;

import com.eventsphere.model.Event;
import com.eventsphere.model.EventCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What the REST API returns for an event. Keeps JPA entities out of the JSON contract. */
public record EventResponse(
        Long id,
        String title,
        String description,
        EventCategory category,
        String venue,
        LocalDateTime startTime,
        int capacity,
        int availableSeats,
        BigDecimal price,
        boolean soldOut) {

    public static EventResponse from(Event e) {
        return new EventResponse(e.getId(), e.getTitle(), e.getDescription(), e.getCategory(),
                e.getVenue(), e.getStartTime(), e.getCapacity(), e.getAvailableSeats(),
                e.getPrice(), e.isSoldOut());
    }
}
