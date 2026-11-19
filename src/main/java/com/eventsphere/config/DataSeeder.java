package com.eventsphere.config;

import com.eventsphere.model.Event;
import com.eventsphere.model.EventCategory;
import com.eventsphere.model.Role;
import com.eventsphere.model.User;
import com.eventsphere.repository.EventRepository;
import com.eventsphere.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Fills an empty database with demo accounts and events so the app is usable straight away.
 * Dates are relative to today, so the sample events are always upcoming.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String studentPassword;

    public DataSeeder(UserRepository userRepository, EventRepository eventRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-password:admin123}") String adminPassword,
                      @Value("${app.seed.student-password:student123}") String studentPassword) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.studentPassword = studentPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(new User("Admin", "admin@eventsphere.com",
                    passwordEncoder.encode(adminPassword), Role.ADMIN));
            userRepository.save(new User("Demo Student", "student@eventsphere.com",
                    passwordEncoder.encode(studentPassword), Role.USER));
            log.info("Seeded demo accounts admin@eventsphere.com and student@eventsphere.com");
        }

        if (eventRepository.count() == 0) {
            eventRepository.saveAll(List.of(
                    new Event("Spring Boot Masterclass",
                            "A hands-on session on building production-ready REST APIs with Spring Boot, "
                                    + "Spring Data JPA and Spring Security. Bring your laptop!",
                            EventCategory.WORKSHOP, "Lab 3, CS Block", at(5, 10, 0), 40, new BigDecimal("149.00")),
                    new Event("CodeSprint 24h Hackathon",
                            "Form a team of up to four and build something amazing in 24 hours. "
                                    + "Mentors, food and prizes included.",
                            EventCategory.HACKATHON, "Innovation Centre", at(12, 9, 0), 120, new BigDecimal("299.00")),
                    new Event("AI in Everyday Life",
                            "Guest lecture on how machine learning powers the apps we use every day, "
                                    + "followed by an open Q&A.",
                            EventCategory.TECH, "Main Auditorium", at(3, 14, 30), 250, BigDecimal.ZERO),
                    new Event("Spring Fest: Cultural Night",
                            "Dance, drama and music performances by student clubs from across the campus.",
                            EventCategory.CULTURAL, "Open Air Theatre", at(20, 18, 0), 500, new BigDecimal("99.00")),
                    new Event("Acoustic Evening",
                            "An intimate unplugged evening with the college music club.",
                            EventCategory.MUSIC, "Seminar Hall B", at(8, 19, 0), 15, new BigDecimal("49.00")),
                    new Event("Inter-College Football League",
                            "Cheer for your college in the opening match of the season.",
                            EventCategory.SPORTS, "University Ground", at(15, 16, 0), 300, BigDecimal.ZERO),
                    new Event("Resume & Interview Bootcamp",
                            "Industry recruiters share what they look for, with live mock interviews.",
                            EventCategory.SEMINAR, "Placement Cell", at(6, 11, 0), 60, BigDecimal.ZERO)));
            log.info("Seeded {} sample events", eventRepository.count());
        }
    }

    private static LocalDateTime at(int daysFromToday, int hour, int minute) {
        return LocalDate.now().plusDays(daysFromToday).atTime(LocalTime.of(hour, minute));
    }
}
