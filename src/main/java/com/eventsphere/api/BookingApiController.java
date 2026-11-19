package com.eventsphere.api;

import com.eventsphere.dto.BookingRequest;
import com.eventsphere.dto.BookingResponse;
import com.eventsphere.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/** REST API for the logged-in user's bookings. All endpoints require authentication. */
@RestController
@RequestMapping("/api/bookings")
public class BookingApiController {

    private final BookingService bookingService;

    public BookingApiController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<BookingResponse> myBookings(Principal principal) {
        return bookingService.bookingsFor(principal.getName()).stream().map(BookingResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody BookingRequest request, Principal principal) {
        return BookingResponse.from(bookingService.book(principal.getName(), request.eventId(), request.seats()));
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, Principal principal) {
        return BookingResponse.from(bookingService.cancel(principal.getName(), id));
    }
}
