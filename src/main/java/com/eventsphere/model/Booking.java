package com.eventsphere.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(nullable = false)
    private int seats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private BookingStatus status = BookingStatus.CONFIRMED;

    /** Human-friendly ticket number shown to the attendee, e.g. EVS-3F9A1C2B. */
    @Column(nullable = false, unique = true, length = 20)
    private String ticketCode;

    @Column(nullable = false, updatable = false)
    private LocalDateTime bookedAt;

    protected Booking() {
        // required by JPA
    }

    public Booking(User user, Event event, int seats, String ticketCode) {
        this.user = user;
        this.event = event;
        this.seats = seats;
        this.ticketCode = ticketCode;
        this.totalAmount = event.getPrice().multiply(BigDecimal.valueOf(seats));
    }

    @PrePersist
    void onCreate() {
        this.bookedAt = LocalDateTime.now();
    }

    public boolean isCancellable() {
        return status == BookingStatus.CONFIRMED && !event.isPast();
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Event getEvent() { return event; }
    public int getSeats() { return seats; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }
    public String getTicketCode() { return ticketCode; }
    public LocalDateTime getBookedAt() { return bookedAt; }
}
