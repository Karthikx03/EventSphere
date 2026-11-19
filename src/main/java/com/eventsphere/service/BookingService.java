package com.eventsphere.service;

import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.ResourceNotFoundException;
import com.eventsphere.model.Booking;
import com.eventsphere.model.BookingStatus;
import com.eventsphere.model.Event;
import com.eventsphere.model.User;
import com.eventsphere.repository.BookingRepository;
import com.eventsphere.repository.EventRepository;
import com.eventsphere.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * All the booking rules live here, so the web UI and the REST API behave exactly the same.
 */
@Service
public class BookingService {

    public static final int MAX_SEATS_PER_BOOKING = 10;

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, EventRepository eventRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Booking book(String userEmail, Long eventId, int seats) {
        if (seats < 1 || seats > MAX_SEATS_PER_BOOKING) {
            throw new BusinessRuleException("You can book between 1 and " + MAX_SEATS_PER_BOOKING + " seats");
        }
        User user = findUser(userEmail);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (event.isPast()) {
            throw new BusinessRuleException("This event has already started");
        }
        if (bookingRepository.existsByUserIdAndEventIdAndStatus(user.getId(), eventId, BookingStatus.CONFIRMED)) {
            throw new BusinessRuleException("You already have a booking for this event");
        }
        if (seats > event.getAvailableSeats()) {
            throw new BusinessRuleException(event.isSoldOut()
                    ? "Sorry, this event is sold out"
                    : "Only " + event.getAvailableSeats() + " seat(s) left");
        }

        event.reserveSeats(seats);
        return bookingRepository.save(new Booking(user, event, seats, newTicketCode()));
    }

    @Transactional
    public Booking cancel(String userEmail, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // Users may only cancel their own bookings. Report "not found" rather than
        // "forbidden" so the API does not reveal which booking ids exist.
        if (!booking.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }
        if (!booking.isCancellable()) {
            throw new BusinessRuleException("This booking can no longer be cancelled");
        }

        booking.cancel();
        booking.getEvent().releaseSeats(booking.getSeats());
        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> bookingsFor(String userEmail) {
        return bookingRepository.findAllForUser(userEmail);
    }

    @Transactional(readOnly = true)
    public List<Booking> bookingsForEvent(Long eventId) {
        return bookingRepository.findAllForEvent(eventId);
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessRuleException("No account found for " + email));
    }

    private static String newTicketCode() {
        return "EVS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
