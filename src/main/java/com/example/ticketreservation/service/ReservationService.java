package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;
import com.example.ticketreservation.security.CustomUserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class ReservationService {

	private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int CODE_LENGTH = 8;

	private final ReservationRepository reservationRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;
	private final com.example.ticketreservation.service.EventService eventService;

	public ReservationService(ReservationRepository reservationRepository, EventRepository eventRepository,
			UserRepository userRepository, EventService eventService) {
		this.reservationRepository = reservationRepository;
		this.eventRepository = eventRepository;
		this.userRepository = userRepository;
		this.eventService = eventService;
	}

	@Transactional
	public ReservationResponse book(ReservationRequest request, String callerUsername) {
		Event event = eventRepository.findById(request.eventId())
				.orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Event not found"));

		// Atomic seat take — returns null if not enough seats remain.
		Event updated = eventService.takeSeats(event.getId(), request.seats());
		if (updated == null) {
			throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, "Not enough available seats");
		}

		String code = generateReservationCode();
		BigDecimal totalAmount = event.getTicketPrice().multiply(BigDecimal.valueOf(request.seats()));

		Reservation reservation = new Reservation();
		reservation.setReservationCode(code);
		reservation.setEventId(event.getId());
		reservation.setEventName(event.getName());
		reservation.setUsername(callerUsername);
		reservation.setSeats(request.seats());
		reservation.setTotalAmount(totalAmount);
		reservation.setStatus(ReservationStatus.CONFIRMED);
		reservation.setCreatedAt(Instant.now());
		reservation.setCancelledAt(null);

		try {
			reservationRepository.save(reservation);
		} catch (Exception ex) {
			// Compensate: release seats if saving the reservation fails.
			eventService.releaseSeats(event.getId(), request.seats());
			throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to create reservation");
		}

		return toResponse(reservation);
	}

	public List<ReservationResponse> myReservations(String username) {
		return reservationRepository.findByUsername(username).stream().map(this::toResponse)
				.collect(Collectors.toList());
	}

	public List<ReservationResponse> listAll(String callerUsername) {
		AppUser caller = userRepository.findByUsername(callerUsername);
		if (caller.getRole() == Role.ADMIN) {
			return reservationRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
		}
		// ORGANIZER: only reservations for their own events.
		List<Event> ownEvents = eventRepository.findByOrganizerUsername(caller.getUsername());
		List<String> ownEventIds = ownEvents.stream().map(Event::getId).collect(Collectors.toList());
		return reservationRepository.findAll().stream().filter(r -> ownEventIds.contains(r.getEventId()))
				.map(this::toResponse).collect(Collectors.toList());
	}

	@Transactional
	public ReservationResponse cancel(String id, String callerUsername) {
		Reservation reservation = reservationRepository.findById(id)
				.orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Reservation not found"));

		AppUser caller = userRepository.findByUsername(callerUsername);
		boolean isAdmin = caller.getRole() == Role.ADMIN;
		boolean isOwner = reservation.getUsername().equals(caller.getUsername());
		if (!isAdmin && !isOwner) {
			throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
					"Customers may only cancel their own reservations");
		}

		if (reservation.getStatus() == ReservationStatus.CANCELLED) {
			throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, "Reservation already cancelled");
		}

		reservation.setStatus(ReservationStatus.CANCELLED);
		reservation.setCancelledAt(Instant.now());
		reservationRepository.save(reservation);

		// Atomically release seats back to the event.
		eventService.releaseSeats(reservation.getEventId(), reservation.getSeats());

		return toResponse(reservation);
	}

	private String generateReservationCode() {
		Random random = new Random();
		StringBuilder sb = new StringBuilder("TKT-");
		for (int i = 0; i < CODE_LENGTH; i++) {
			sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
		}
		return sb.toString();
	}

	private ReservationResponse toResponse(Reservation reservation) {
		return new ReservationResponse(reservation.getId(), reservation.getReservationCode(), reservation.getEventId(),
				reservation.getEventName(), reservation.getUsername(), reservation.getSeats(),
				reservation.getTotalAmount(), reservation.getStatus(), reservation.getCreatedAt(),
				reservation.getCancelledAt());
	}
}