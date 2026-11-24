package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ReservationResponse> book(@Valid @RequestBody ReservationRequest request,
			org.springframework.security.core.Authentication auth) {
		return ResponseEntity.status(201).body(reservationService.book(request, auth.getName()));
	}

	@GetMapping("/my")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<List<ReservationResponse>> myReservations(
			org.springframework.security.core.Authentication auth) {
		return ResponseEntity.ok(reservationService.myReservations(auth.getName()));
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
	public ResponseEntity<List<ReservationResponse>> list(
			org.springframework.security.core.Authentication auth) {
		return ResponseEntity.ok(reservationService.listAll(auth.getName()));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
	public ResponseEntity<ReservationResponse> cancel(@PathVariable String id,
			org.springframework.security.core.Authentication auth) {
		return ResponseEntity.ok(reservationService.cancel(id, auth.getName()));
	}
}
