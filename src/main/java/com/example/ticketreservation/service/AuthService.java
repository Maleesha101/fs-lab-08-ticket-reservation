package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.AuthResponse;
import com.example.ticketreservation.dto.LoginRequest;
import com.example.ticketreservation.dto.RegisterRequest;
import com.example.ticketreservation.dto.UserResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.UserRepository;
import com.example.ticketreservation.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public UserResponse register(RegisterRequest request) {
		Role requestedRole = request.role() != null ? request.role() : Role.CUSTOMER;

		// Elevating to ADMIN or ORGANIZER requires an authenticated ADMIN caller.
		if (requestedRole != Role.CUSTOMER) {
			AppUser caller = currentAuthenticatedUser();
			if (caller == null || caller.getRole() != Role.ADMIN) {
				throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
						"Only an authenticated ADMIN may create " + requestedRole + " accounts");
			}
		}

		if (userRepository.findByUsername(request.username()) != null) {
			throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, "Username already exists");
		}

		AppUser user = new AppUser();
		user.setUsername(request.username());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFullName(request.fullName());
		user.setRole(requestedRole);
		user.setCreatedAt(Instant.now());
		userRepository.save(user);

		return toResponse(user);
	}

	public AuthResponse login(LoginRequest request) {
		AppUser user = userRepository.findByUsername(request.username());
		if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BadCredentialsException("Invalid username or password");
		}
		String token = jwtService.generateToken(user);
		return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), user.getUsername(), user.getFullName(),
				user.getRole());
	}

	public UserResponse me(String username) {
		AppUser user = userRepository.findByUsername(username);
		if (user == null) {
			throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found");
		}
		return toResponse(user);
	}

	private AppUser currentAuthenticatedUser() {
		var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof AppUser)) {
			return null;
		}
		return (AppUser) auth.getPrincipal();
	}

	private UserResponse toResponse(AppUser user) {
		return new UserResponse(user.getUsername(), user.getFullName(), user.getRole(), user.getCreatedAt());
	}
}
