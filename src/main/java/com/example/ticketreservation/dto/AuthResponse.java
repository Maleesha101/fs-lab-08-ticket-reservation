package com.example.ticketreservation.dto;

import com.example.ticketreservation.model.Role;

public record AuthResponse(
		String token,
		String tokenType,
		Long expiresInMs,
		String username,
		String fullName,
		Role role
) {
}