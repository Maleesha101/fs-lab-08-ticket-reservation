package com.example.ticketreservation.repository;

import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
	List<Reservation> findByUsername(String username);

	List<Reservation> findByEventId(String eventId);

	long countByEventId(String eventId);

	long countByStatus(ReservationStatus status);

	long countByEventIdAndStatus(String eventId, ReservationStatus status);
}