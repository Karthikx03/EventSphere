package com.eventsphere.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventCategory category;

    @Column(nullable = false, length = 120)
    private String venue;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int seatsBooked;

    @Column(nullable = false, precision = 9, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optimistic locking: if two people book the last seat at the same moment,
     * one transaction fails instead of the event being overbooked.
     */
    @Version
    private Long version;

    protected Event() {
        // required by JPA
    }

    public Event(String title, String description, EventCategory category, String venue,
                 LocalDateTime startTime, int capacity, BigDecimal price) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.venue = venue;
        this.startTime = startTime;
        this.capacity = capacity;
        this.price = price;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- domain behaviour -------------------------------------------------

    public int getAvailableSeats() {
        return capacity - seatsBooked;
    }

    public boolean isSoldOut() {
        return getAvailableSeats() <= 0;
    }

    public boolean isPast() {
        return startTime.isBefore(LocalDateTime.now());
    }

    public boolean isFree() {
        return price.signum() == 0;
    }

    /** Percentage of seats already taken, used for the progress bar in the UI. */
    public int getOccupancyPercent() {
        return capacity == 0 ? 100 : (int) Math.round(seatsBooked * 100.0 / capacity);
    }

    public void reserveSeats(int seats) {
        if (seats > getAvailableSeats()) {
            throw new IllegalStateException("Not enough seats available");
        }
        this.seatsBooked += seats;
    }

    public void releaseSeats(int seats) {
        this.seatsBooked = Math.max(0, this.seatsBooked - seats);
    }

    // ---- getters / setters ------------------------------------------------

    public Long getId() { return id; }
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
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getSeatsBooked() { return seatsBooked; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}
