package com.example.ticketreservation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "reservations")
public class Reservation {

	@Indexed
	private String id;

	@Indexed(unique = true)
	private String reservationCode;

	private String eventId;

	private String eventName;

	private String username;

	private Integer seats;

	private BigDecimal totalAmount;

	private ReservationStatus status;

	private Instant createdAt;

	private Instant cancelledAt;

	public Reservation() {
	}

	public Reservation(String reservationCode, String eventId, String eventName, String username, Integer seats,
			BigDecimal totalAmount, ReservationStatus status, Instant createdAt, Instant cancelledAt) {
		this.id = reservationCode;
		this.reservationCode = reservationCode;
		this.eventId = eventId;
		this.eventName = eventName;
		this.username = username;
		this.seats = seats;
		this.totalAmount = totalAmount;
		this.status = status;
		this.createdAt = createdAt;
		this.cancelledAt = cancelledAt;
	}

	public String getId() {
		return id;
	}

	public String getReservationCode() {
		return reservationCode;
	}

	public void setReservationCode(String reservationCode) {
		this.reservationCode = reservationCode;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Integer getSeats() {
		return seats;
	}

	public void setSeats(Integer seats) {
		this.seats = seats;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public void setStatus(ReservationStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

	public void setCancelledAt(Instant cancelledAt) {
		this.cancelledAt = cancelledAt;
	}
}
