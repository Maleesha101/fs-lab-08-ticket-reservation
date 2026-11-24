package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.StatsResponse;
import com.example.ticketreservation.dto.UserResponse;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.repository.UserRepository;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final ReservationRepository reservationRepository;

	public AdminService(UserRepository userRepository, EventRepository eventRepository,
			ReservationRepository reservationRepository) {
		this.userRepository = userRepository;
		this.eventRepository = eventRepository;
		this.reservationRepository = reservationRepository;
	}

	public List<UserResponse> listUsers() {
		return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
	}

	public StatsResponse stats() {
		long totalUsers = userRepository.count();
		long totalEvents = eventRepository.count();
		long totalReservations = reservationRepository.countByStatus(ReservationStatus.CONFIRMED);
		long ticketsSold = reservationRepository.findAll().stream()
				.filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).mapToInt(r -> r.getSeats()).sum();
		BigDecimal revenue = reservationRepository.findAll().stream()
				.filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).map(r -> r.getTotalAmount())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new StatsResponse(totalUsers, totalEvents, totalReservations, ticketsSold, revenue);
	}

	private UserResponse toResponse(AppUser user) {
		return new UserResponse(user.getUsername(), user.getFullName(), user.getRole(), user.getCreatedAt());
	}
}