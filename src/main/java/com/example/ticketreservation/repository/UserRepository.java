package com.example.ticketreservation.repository;

import com.example.ticketreservation.model.AppUser;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<AppUser, String> {
	AppUser findByUsername(String username);
}