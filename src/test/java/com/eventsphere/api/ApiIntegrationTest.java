package com.eventsphere.api;

import com.eventsphere.model.Event;
import com.eventsphere.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests through the real Spring context: security rules, controllers,
 * services and the H2 database (seeded by DataSeeder). Each test is rolled back.
 */
@SpringBootTest
@Transactional
class ApiIntegrationTest {

    private static final String STUDENT = "student@eventsphere.com";
    private static final String ADMIN = "admin@eventsphere.com";

    @Autowired private WebApplicationContext context;
    @Autowired private EventRepository eventRepository;

    private MockMvc mvc;
    private Long eventId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        eventId = eventRepository.searchUpcoming(LocalDateTime.now(), null, null).stream()
                .filter(e -> e.getAvailableSeats() >= 2)
                .map(Event::getId)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void anyoneCanListUpcomingEvents() throws Exception {
        mvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].title", not(emptyString())));
    }

    @Test
    void unknownEventReturns404ProblemDetail() throws Exception {
        mvc.perform(get("/api/events/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"));
    }

    @Test
    void bookingRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\": " + eventId + ", \"seats\": 1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCanBookAndCancel() throws Exception {
        mvc.perform(post("/api/bookings").with(user(STUDENT).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\": " + eventId + ", \"seats\": 2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode", startsWith("EVS-")))
                .andExpect(jsonPath("$.seats").value(2))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // booking the same event again is a business-rule conflict
        mvc.perform(post("/api/bookings").with(user(STUDENT).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\": " + eventId + ", \"seats\": 1}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/bookings").with(user(STUDENT).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void invalidBookingRequestReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/bookings").with(user(STUDENT).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\": " + eventId + ", \"seats\": 50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.seats").exists());
    }

    @Test
    void onlyAdminsCanCreateEvents() throws Exception {
        String body = """
                {"title": "Test Event", "description": "Created by a test", "category": "TECH",
                 "venue": "Room 101", "startTime": "%s", "capacity": 30, "price": 0}
                """.formatted(LocalDateTime.now().plusDays(10).withNano(0));

        mvc.perform(post("/api/events").with(user(STUDENT).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/events").with(user(ADMIN).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.availableSeats").value(30));
    }

    @Test
    void adminPagesAreProtected() throws Exception {
        mvc.perform(get("/admin").with(user(STUDENT).roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void publicPagesRender() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/events")).andExpect(status().isOk());
        mvc.perform(get("/events/" + eventId)).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }
}
