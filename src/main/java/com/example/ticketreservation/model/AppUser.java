package com.example.ticketreservation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
public class AppUser {

	@Id
	private String id;

	@Indexed(unique = true)
	private String username;

	private String passwordHash;

	private String fullName;

	private Role role;

	private Instant createdAt;

	public AppUser() {
	}

	public AppUser(String username, String passwordHash, String fullName, Role role, Instant createdAt) {
		this.id = username;
		this.username = username;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.role = role;
		this.createdAt = createdAt;
	}

	public String getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
