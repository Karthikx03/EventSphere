package com.eventsphere.dto;

import java.math.BigDecimal;

public record DashboardStats(long totalEvents, long upcomingEvents, long confirmedBookings,
                             long registeredUsers, BigDecimal revenue) {
}
