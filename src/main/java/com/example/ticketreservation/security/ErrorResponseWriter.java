package com.example.ticketreservation.security;

import com.example.ticketreservation.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ErrorResponseWriter {

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

	public void writeError(HttpServletResponse response, int status, String error, String message) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		ErrorResponse err = new ErrorResponse(Instant.now(), status, error, message, null);
		OutputStream out = response.getOutputStream();
		out.write(objectMapper.writeValueAsBytes(err));
		out.flush();
	}
}
