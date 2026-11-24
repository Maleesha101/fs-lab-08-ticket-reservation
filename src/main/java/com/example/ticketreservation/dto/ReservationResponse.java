package com.example.ticketreservation.dto;

import com.example.ticketreservation.model.ReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ReservationResponse(
		String id,
		String reservationCode,
		String eventId,
		String eventName,
		String username,
		Integer seats,
		BigDecimal totalAmount,
		ReservationStatus status,
		Instant createdAt,
		Instant cancelledAt
) {
}