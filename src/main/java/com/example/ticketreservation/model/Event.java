package com.example.ticketreservation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Document(collection = "events")
public class Event {

	@Id
	private String id;

	@Indexed
	private String name;

	private String description;

	private String venue;

	private LocalDateTime eventDate;

	private Integer totalSeats;

	private Integer availableSeats;

	private BigDecimal ticketPrice;

	private String organizerUsername;

	private Instant createdAt;

	public Event() {
	}

	public Event(String name, String description, String venue, LocalDateTime eventDate, Integer totalSeats,
			Integer availableSeats, BigDecimal ticketPrice, String organizerUsername, Instant createdAt) {
		this.id = "evt_" + Math.abs(name.hashCode());
		this.name = name;
		this.description = description;
		this.venue = venue;
		this.eventDate = eventDate;
		this.totalSeats = totalSeats;
		this.availableSeats = availableSeats;
		this.ticketPrice = ticketPrice;
		this.organizerUsername = organizerUsername;
		this.createdAt = createdAt;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getVenue() {
		return venue;
	}

	public void setVenue(String venue) {
		this.venue = venue;
	}

	public LocalDateTime getEventDate() {
		return eventDate;
	}

	public void setEventDate(LocalDateTime eventDate) {
		this.eventDate = eventDate;
	}

	public Integer getTotalSeats() {
		return totalSeats;
	}

	public void setTotalSeats(Integer totalSeats) {
		this.totalSeats = totalSeats;
	}

	public Integer getAvailableSeats() {
		return availableSeats;
	}

	public void setAvailableSeats(Integer availableSeats) {
		this.availableSeats = availableSeats;
	}

	public BigDecimal getTicketPrice() {
		return ticketPrice;
	}

	public void setTicketPrice(BigDecimal ticketPrice) {
		this.ticketPrice = ticketPrice;
	}

	public String getOrganizerUsername() {
		return organizerUsername;
	}

	public void setOrganizerUsername(String organizerUsername) {
		this.organizerUsername = organizerUsername;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
