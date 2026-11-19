package com.eventsphere.web;

import com.eventsphere.dto.EventForm;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.model.Event;
import com.eventsphere.service.BookingService;
import com.eventsphere.service.EventService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin back office. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final EventService eventService;
    private final BookingService bookingService;

    public AdminController(EventService eventService, BookingService bookingService) {
        this.eventService = eventService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", eventService.stats());
        model.addAttribute("events", eventService.findAll());
        return "admin/dashboard";
    }

    @GetMapping("/events/new")
    public String newEvent(Model model) {
        model.addAttribute("form", new EventForm());
        model.addAttribute("eventId", null);
        return "admin/event-form";
    }

    @PostMapping("/events")
    public String create(@Valid @ModelAttribute("form") EventForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("eventId", null);
            return "admin/event-form";
        }
        Event event = eventService.create(form);
        redirect.addFlashAttribute("success", "Event \"" + event.getTitle() + "\" created.");
        return "redirect:/admin";
    }

    @GetMapping("/events/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", eventService.toForm(eventService.getById(id)));
        model.addAttribute("eventId", id);
        return "admin/event-form";
    }

    @PostMapping("/events/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") EventForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                eventService.update(id, form);
                redirect.addFlashAttribute("success", "Event updated.");
                return "redirect:/admin";
            } catch (BusinessRuleException ex) {
                result.rejectValue("capacity", "capacity", ex.getMessage());
            }
        }
        model.addAttribute("eventId", id);
        return "admin/event-form";
    }

    @PostMapping("/events/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            eventService.delete(id);
            redirect.addFlashAttribute("success", "Event deleted.");
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin";
    }

    @GetMapping("/events/{id}/bookings")
    public String eventBookings(@PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.getById(id));
        model.addAttribute("bookings", bookingService.bookingsForEvent(id));
        return "admin/event-bookings";
    }
}
