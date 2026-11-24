package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

	private final EventRepository eventRepository;
	private final ReservationRepository reservationRepository;
	private final MongoTemplate mongoTemplate;
	private final UserRepository userRepository;

	public EventService(EventRepository eventRepository, ReservationRepository reservationRepository,
			MongoTemplate mongoTemplate, UserRepository userRepository) {
		this.eventRepository = eventRepository;
		this.reservationRepository = reservationRepository;
		this.mongoTemplate = mongoTemplate;
		this.userRepository = userRepository;
	}

	public EventResponse create(EventRequest request, String callerUsername) {
		AppUser caller = userRepository.findByUsername(callerUsername);
		Event event = new Event();
		event.setName(request.name());
		event.setDescription(request.description());
		event.setVenue(request.venue());
		event.setEventDate(request.eventDate());
		event.setTotalSeats(request.totalSeats());
		event.setAvailableSeats(request.totalSeats());
		event.setTicketPrice(request.ticketPrice());
		event.setOrganizerUsername(caller.getUsername());
		event.setCreatedAt(Instant.now());
		eventRepository.save(event);
		return toResponse(event);
	}

	public List<EventResponse> list(String name) {
		List<Event> events;
		if (name == null || name.isBlank()) {
			events = eventRepository.findAll();
		} else {
			events = eventRepository.findAll().stream()
					.filter(e -> e.getName() != null && e.getName().toLowerCase().contains(name.toLowerCase()))
					.collect(Collectors.toList());
		}
		return events.stream().map(this::toResponse).collect(Collectors.toList());
	}

	public EventResponse getById(String id) {
		Event event = eventRepository.findById(id)
				.orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Event not found"));
		return toResponse(event);
	}

	public EventResponse update(String id, EventRequest request, String callerUsername) {
		Event event = eventRepository.findById(id)
				.orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Event not found"));

		AppUser caller = userRepository.findByUsername(callerUsername);
		boolean isAdmin = caller.getRole() == Role.ADMIN;
		boolean isOwner = event.getOrganizerUsername().equals(caller.getUsername());
		if (!isAdmin && !isOwner) {
			throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
					"Organizer may only edit their own events");
		}

		long soldSeats = reservationRepository.countByEventIdAndStatus(id,
				com.example.ticketreservation.model.ReservationStatus.CONFIRMED);
		int newTotal = request.totalSeats() != null ? request.totalSeats() : event.getTotalSeats();
		if (newTotal < soldSeats) {
			throw new ApiException(org.springframework.http.HttpStatus.CONFLICT,
					"totalSeats cannot be below already-sold seats (" + soldSeats + ")");
		}

		event.setName(request.name() != null ? request.name() : event.getName());
		event.setDescription(request.description() != null ? request.description() : event.getDescription());
		event.setVenue(request.venue() != null ? request.venue() : event.getVenue());
		event.setEventDate(request.eventDate() != null ? request.eventDate() : event.getEventDate());
		event.setTicketPrice(request.ticketPrice() != null ? request.ticketPrice() : event.getTicketPrice());

		int oldAvailable = event.getAvailableSeats();
		int oldTotal = event.getTotalSeats();
		event.setTotalSeats(newTotal);
		// Adjust availableSeats consistently with the change in totalSeats.
		int newAvailable = oldAvailable + (newTotal - oldTotal);
		if (newAvailable < 0) {
			newAvailable = 0;
		}
		if (newAvailable > newTotal) {
			newAvailable = newTotal;
		}
		event.setAvailableSeats(newAvailable);

		eventRepository.save(event);
		return toResponse(event);
	}

	public void delete(String id) {
		Event event = eventRepository.findById(id)
				.orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Event not found"));
		long reservations = reservationRepository.countByEventId(id);
		if (reservations > 0) {
			throw new ApiException(org.springframework.http.HttpStatus.CONFLICT,
					"Event has existing reservations and cannot be deleted");
		}
		eventRepository.delete(event);
	}

	/**
	 * Atomically take `seats` seats from the event identified by eventId. Uses a
	 * findAndModify with an availableSeats >= seats guard so that concurrent
	 * bookings cannot oversell. Returns the updated Event, or null if not enough
	 * seats remain (or event missing).
	 */
	public Event takeSeats(String eventId, int seats) {
		Query query = new Query(Criteria.where("_id").is(eventId).and("availableSeats").gte(seats));
		Update update = new Update().inc("availableSeats", -seats);
		return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Event.class);
	}

	/**
	 * Atomically release `seats` seats back to the event, guarded so availableSeats
	 * cannot exceed totalSeats.
	 */
	public void releaseSeats(String eventId, int seats) {
		Event current = mongoTemplate.findById(eventId, Event.class);
		if (current != null) {
			Query query = new Query(Criteria.where("_id").is(eventId).and("availableSeats").lte(
					current.getTotalSeats() - seats));
			Update update = new Update().inc("availableSeats", seats);
			mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Event.class);
		}
	}

	public EventResponse toResponse(Event event) {
		return new EventResponse(event.getId(), event.getName(), event.getDescription(), event.getVenue(),
				event.getEventDate(), event.getTotalSeats(), event.getAvailableSeats(), event.getTicketPrice(),
				event.getOrganizerUsername(), event.getCreatedAt());
	}
}