package com.eventsphere.service;

import com.eventsphere.dto.DashboardStats;
import com.eventsphere.dto.EventForm;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.ResourceNotFoundException;
import com.eventsphere.model.BookingStatus;
import com.eventsphere.model.Event;
import com.eventsphere.model.EventCategory;
import com.eventsphere.repository.BookingRepository;
import com.eventsphere.repository.EventRepository;
import com.eventsphere.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, BookingRepository bookingRepository,
                        UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    public List<Event> searchUpcoming(String query, EventCategory category) {
        String q = StringUtils.hasText(query) ? query.trim() : null;
        return eventRepository.searchUpcoming(LocalDateTime.now(), q, category);
    }

    public List<Event> featured() {
        return eventRepository.findTop3ByStartTimeAfterOrderByStartTimeAsc(LocalDateTime.now());
    }

    public List<Event> findAll() {
        return eventRepository.findAllByOrderByStartTimeDesc();
    }

    public Event getById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    @Transactional
    public Event create(EventForm form) {
        Event event = new Event(form.getTitle().trim(), form.getDescription().trim(), form.getCategory(),
                form.getVenue().trim(), form.getStartTime(), form.getCapacity(), form.getPrice());
        return eventRepository.save(event);
    }

    @Transactional
    public Event update(Long id, EventForm form) {
        Event event = getById(id);
        if (form.getCapacity() < event.getSeatsBooked()) {
            throw new BusinessRuleException("Capacity cannot be lower than the "
                    + event.getSeatsBooked() + " seats already booked");
        }
        event.setTitle(form.getTitle().trim());
        event.setDescription(form.getDescription().trim());
        event.setCategory(form.getCategory());
        event.setVenue(form.getVenue().trim());
        event.setStartTime(form.getStartTime());
        event.setCapacity(form.getCapacity());
        event.setPrice(form.getPrice());
        return event; // dirty checking writes the changes on commit
    }

    @Transactional
    public void delete(Long id) {
        Event event = getById(id);
        if (bookingRepository.countByEventId(id) > 0) {
            throw new BusinessRuleException("This event already has bookings and cannot be deleted");
        }
        eventRepository.delete(event);
    }

    public EventForm toForm(Event event) {
        EventForm form = new EventForm();
        form.setTitle(event.getTitle());
        form.setDescription(event.getDescription());
        form.setCategory(event.getCategory());
        form.setVenue(event.getVenue());
        form.setStartTime(event.getStartTime());
        form.setCapacity(event.getCapacity());
        form.setPrice(event.getPrice());
        return form;
    }

    public DashboardStats stats() {
        BigDecimal revenue = bookingRepository.totalRevenue();
        return new DashboardStats(
                eventRepository.count(),
                eventRepository.countByStartTimeAfter(LocalDateTime.now()),
                bookingRepository.countByStatus(BookingStatus.CONFIRMED),
                userRepository.count(),
                revenue == null ? BigDecimal.ZERO : revenue);
    }
}
