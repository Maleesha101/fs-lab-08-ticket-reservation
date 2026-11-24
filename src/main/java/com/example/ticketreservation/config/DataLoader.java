package com.example.ticketreservation.config;

import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final ReservationRepository reservationRepository;
	private final PasswordEncoder passwordEncoder;

	public DataLoader(UserRepository userRepository, EventRepository eventRepository,
			ReservationRepository reservationRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.eventRepository = eventRepository;
		this.reservationRepository = reservationRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		if (userRepository.count() > 0 || eventRepository.count() > 0 || reservationRepository.count() > 0) {
			return;
		}

		AppUser admin = new AppUser();
		admin.setUsername("admin");
		admin.setPasswordHash(passwordEncoder.encode("admin123"));
		admin.setFullName("System Administrator");
		admin.setRole(Role.ADMIN);
		admin.setCreatedAt(Instant.now());
		userRepository.save(admin);

		AppUser organizer = new AppUser();
		organizer.setUsername("organizer");
		organizer.setPasswordHash(passwordEncoder.encode("organizer123"));
		organizer.setFullName("Event Organizer");
		organizer.setRole(Role.ORGANIZER);
		organizer.setCreatedAt(Instant.now());
		userRepository.save(organizer);

		AppUser customer = new AppUser();
		customer.setUsername("customer");
		customer.setPasswordHash(passwordEncoder.encode("customer123"));
		customer.setFullName("Sample Customer");
		customer.setRole(Role.CUSTOMER);
		customer.setCreatedAt(Instant.now());
		userRepository.save(customer);

		// Three sample events owned by `organizer`, with future dates.
		Instant now = Instant.now();
		Event e1 = new Event();
		e1.setName("Tech Conference 2026");
		e1.setDescription("Annual technology conference");
		e1.setVenue("Main Hall");
		e1.setEventDate(LocalDateTime.now().plusDays(10));
		e1.setTotalSeats(200);
		e1.setAvailableSeats(200);
		e1.setTicketPrice(new BigDecimal("5000.00"));
		e1.setOrganizerUsername("organizer");
		e1.setCreatedAt(now);
		eventRepository.save(e1);

		Event e2 = new Event();
		e2.setName("Music Festival");
		e2.setDescription("Open-air music festival");
		e2.setVenue("City Grounds");
		e2.setEventDate(LocalDateTime.now().plusDays(25));
		e2.setTotalSeats(500);
		e2.setAvailableSeats(500);
		e2.setTicketPrice(new BigDecimal("3500.00"));
		e2.setOrganizerUsername("organizer");
		e2.setCreatedAt(now);
		eventRepository.save(e2);

		Event e3 = new Event();
		e3.setName("Coding Bootcamp");
		e3.setDescription("Intensive coding bootcamp");
		e3.setVenue("Innovation Lab");
		e3.setEventDate(LocalDateTime.now().plusDays(40));
		e3.setTotalSeats(100);
		e3.setAvailableSeats(100);
		e3.setTicketPrice(new BigDecimal("2000.00"));
		e3.setOrganizerUsername("organizer");
		e3.setCreatedAt(now);
		eventRepository.save(e3);
	}
}