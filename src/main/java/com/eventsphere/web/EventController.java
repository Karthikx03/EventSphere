package com.eventsphere.web;

import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.model.Booking;
import com.eventsphere.model.Event;
import com.eventsphere.model.EventCategory;
import com.eventsphere.service.BookingService;
import com.eventsphere.service.EventService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final BookingService bookingService;

    public EventController(EventService eventService, BookingService bookingService) {
        this.eventService = eventService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) EventCategory category,
                       Model model) {
        model.addAttribute("events", eventService.searchUpcoming(q, category));
        model.addAttribute("q", q);
        model.addAttribute("selectedCategory", category);
        return "events/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Event event = eventService.getById(id);
        model.addAttribute("event", event);
        // the seat picker never offers more than the per-booking limit or the seats left
        model.addAttribute("maxSeats", Math.max(1, Math.min(BookingService.MAX_SEATS_PER_BOOKING, event.getAvailableSeats())));
        return "events/detail";
    }

    @PostMapping("/{id}/book")
    public String book(@PathVariable Long id, @RequestParam(defaultValue = "1") int seats,
                       Principal principal, RedirectAttributes redirect) {
        try {
            Booking booking = bookingService.book(principal.getName(), id, seats);
            redirect.addFlashAttribute("success",
                    "Booked! Your ticket code is " + booking.getTicketCode() + ".");
            return "redirect:/bookings";
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (ObjectOptimisticLockingFailureException ex) {
            redirect.addFlashAttribute("error", "Seats changed while you were booking. Please try again.");
        }
        return "redirect:/events/" + id;
    }
}
