package com.example.ticketreservation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class MongoTestConfig {
	// placeholder — test MongoDB is wired via @TestConfiguration in tests
}