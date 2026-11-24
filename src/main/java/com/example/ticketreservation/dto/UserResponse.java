package com.example.ticketreservation.dto;

import com.example.ticketreservation.model.Role;
import java.time.Instant;

public record UserResponse(
		String username,
		String fullName,
		Role role,
		Instant createdAt
) {
}