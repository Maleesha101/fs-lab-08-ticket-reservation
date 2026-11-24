package com.example.ticketreservation.security;

import com.example.ticketreservation.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		ErrorResponse error = new ErrorResponse(java.time.Instant.now(), HttpServletResponse.SC_FORBIDDEN, "Forbidden",
				accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "Access denied",
				request.getRequestURI());
		response.getWriter().write(objectMapper.writeValueAsString(error));
	}
}
