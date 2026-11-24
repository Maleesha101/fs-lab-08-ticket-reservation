package com.example.ticketreservation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventRequest(
		@NotBlank String name,
		String description,
		@NotBlank String venue,
		@Future LocalDateTime eventDate,
		@Min(1) Integer totalSeats,
		@DecimalMin("0.0") BigDecimal ticketPrice
) {
}