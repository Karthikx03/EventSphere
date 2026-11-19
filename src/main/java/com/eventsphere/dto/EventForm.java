package com.eventsphere.dto;

import com.eventsphere.model.EventCategory;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Input for creating or editing an event, used by both the admin form and the REST API. */
public class EventForm {

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must be at most 120 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @NotNull(message = "Choose a category")
    private EventCategory category;

    @NotBlank(message = "Venue is required")
    @Size(max = 120, message = "Venue must be at most 120 characters")
    private String venue;

    @NotNull(message = "Start date and time are required")
    @Future(message = "The event must be in the future")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 10000, message = "Capacity can be at most 10,000")
    private Integer capacity;

    @NotNull(message = "Price is required (use 0 for a free event)")
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    @Digits(integer = 7, fraction = 2, message = "Price can have at most 2 decimal places")
    private BigDecimal price;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public EventCategory getCategory() { return category; }
    public void setCategory(EventCategory category) { this.category = category; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
