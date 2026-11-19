package com.eventsphere.service;

import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.ResourceNotFoundException;
import com.eventsphere.model.*;
import com.eventsphere.repository.BookingRepository;
import com.eventsphere.repository.EventRepository;
import com.eventsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Pure unit tests of the booking rules: no Spring context, repositories are mocked. */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final String EMAIL = "student@test.com";

    @Mock private BookingRepository bookingRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private BookingService bookingService;

    private User user;
    private Event event;

    @BeforeEach
    void setUp() {
        user = new User("Student", EMAIL, "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        event = new Event("Workshop", "desc", EventCategory.WORKSHOP, "Lab 1",
                LocalDateTime.now().plusDays(3), 5, new BigDecimal("100.00"));
        ReflectionTestUtils.setField(event, "id", 10L);
    }

    private void givenUserAndEvent() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
    }

    @Test
    void bookingReservesSeatsAndCalculatesTotal() {
        givenUserAndEvent();
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = bookingService.book(EMAIL, 10L, 2);

        assertThat(booking.getSeats()).isEqualTo(2);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("200.00");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getTicketCode()).startsWith("EVS-").hasSize(12);
        assertThat(event.getAvailableSeats()).isEqualTo(3);
    }

    @Test
    void cannotBookMoreSeatsThanAvailable() {
        givenUserAndEvent();

        assertThatThrownBy(() -> bookingService.book(EMAIL, 10L, 6))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only 5 seat(s) left");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cannotBookSoldOutEvent() {
        event.reserveSeats(5);
        givenUserAndEvent();

        assertThatThrownBy(() -> bookingService.book(EMAIL, 10L, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sold out");
    }

    @Test
    void cannotBookSameEventTwice() {
        givenUserAndEvent();
        when(bookingRepository.existsByUserIdAndEventIdAndStatus(1L, 10L, BookingStatus.CONFIRMED)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.book(EMAIL, 10L, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already have a booking");
    }

    @Test
    void cannotBookPastEvent() {
        event.setStartTime(LocalDateTime.now().minusHours(1));
        givenUserAndEvent();

        assertThatThrownBy(() -> bookingService.book(EMAIL, 10L, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already started");
    }

    @Test
    void rejectsInvalidSeatCountBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> bookingService.book(EMAIL, 10L, 0)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> bookingService.book(EMAIL, 10L, 11)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(userRepository, eventRepository, bookingRepository);
    }

    @Test
    void cancellingReleasesSeats() {
        event.reserveSeats(2);
        Booking booking = new Booking(user, event, 2, "EVS-TEST0001");
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        bookingService.cancel(EMAIL, 99L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(event.getAvailableSeats()).isEqualTo(5);
    }

    @Test
    void cannotCancelSomeoneElsesBooking() {
        Booking booking = new Booking(user, event, 1, "EVS-TEST0002");
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel("intruder@test.com", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
