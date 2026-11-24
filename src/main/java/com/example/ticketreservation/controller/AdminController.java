package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.StatsResponse;
import com.example.ticketreservation.dto.UserResponse;
import com.example.ticketreservation.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@GetMapping("/users")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<UserResponse>> listUsers() {
		return ResponseEntity.ok(adminService.listUsers());
	}

	@GetMapping("/stats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<StatsResponse> stats() {
		return ResponseEntity.ok(adminService.stats());
	}
}
