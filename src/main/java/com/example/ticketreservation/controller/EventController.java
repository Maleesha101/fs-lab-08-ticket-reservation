package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping
	public ResponseEntity<List<EventResponse>> list(@RequestParam(required = false) String name) {
		return ResponseEntity.ok(eventService.list(name));
	}

	@GetMapping("/{id}")
	public ResponseEntity<EventResponse> getById(@PathVariable String id) {
		return ResponseEntity.ok(eventService.getById(id));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
	public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request, org.springframework.security.core.Authentication auth) {
		return ResponseEntity.status(201).body(eventService.create(request, auth.getName()));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
	public ResponseEntity<EventResponse> update(@PathVariable String id, @Valid @RequestBody EventRequest request,
			org.springframework.security.core.Authentication auth) {
		return ResponseEntity.ok(eventService.update(id, request, auth.getName()));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		eventService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
