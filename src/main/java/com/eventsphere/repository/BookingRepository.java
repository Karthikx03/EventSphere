package com.eventsphere.repository;

import com.eventsphere.model.Booking;
import com.eventsphere.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** "join fetch" loads the event in the same query, avoiding the N+1 problem. */
    @Query("select b from Booking b join fetch b.event where b.user.email = :email order by b.bookedAt desc")
    List<Booking> findAllForUser(@Param("email") String email);

    @Query("select b from Booking b join fetch b.user where b.event.id = :eventId order by b.bookedAt desc")
    List<Booking> findAllForEvent(@Param("eventId") Long eventId);

    boolean existsByUserIdAndEventIdAndStatus(Long userId, Long eventId, BookingStatus status);

    long countByStatus(BookingStatus status);

    long countByEventId(Long eventId);

    /** Returns null when there are no confirmed bookings yet. */
    @Query("select sum(b.totalAmount) from Booking b where b.status = com.eventsphere.model.BookingStatus.CONFIRMED")
    BigDecimal totalRevenue();
}
