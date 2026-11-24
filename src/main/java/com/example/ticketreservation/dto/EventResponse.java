package com.example.ticketreservation.dto;

import com.example.ticketreservation.model.ReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record EventResponse(
		String id,
		String name,
		String description,
		String venue,
		LocalDateTime eventDate,
		Integer totalSeats,
		Integer availableSeats,
		BigDecimal ticketPrice,
		String organizerUsername,
		Instant createdAt
) {
}