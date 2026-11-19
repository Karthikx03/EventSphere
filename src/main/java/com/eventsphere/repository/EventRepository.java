package com.eventsphere.repository;

import com.eventsphere.model.Event;
import com.eventsphere.model.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Upcoming events, optionally filtered by a search term (title or venue) and a category.
     * A null parameter means "no filter".
     */
    @Query("""
            select e from Event e
            where e.startTime > :now
              and (:category is null or e.category = :category)
              and (:q is null
                   or lower(e.title) like lower(concat('%', :q, '%'))
                   or lower(e.venue) like lower(concat('%', :q, '%')))
            order by e.startTime asc
            """)
    List<Event> searchUpcoming(@Param("now") LocalDateTime now,
                               @Param("q") String q,
                               @Param("category") EventCategory category);

    List<Event> findTop3ByStartTimeAfterOrderByStartTimeAsc(LocalDateTime now);

    List<Event> findAllByOrderByStartTimeDesc();

    long countByStartTimeAfter(LocalDateTime now);
}
