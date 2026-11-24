package com.example.ticketreservation.exception;

import com.example.ticketreservation.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining("; "));
		ErrorResponse error = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Validation failed", message,
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Malformed JSON",
				ex.getMessage() != null ? ex.getMessage() : "Unreadable request body", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse(Instant.now(), ex.getStatus().value(), ex.getStatus().getReasonPhrase(),
				ex.getMessage(), request.getRequestURI());
		return ResponseEntity.status(ex.getStatus()).body(error);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse(Instant.now(), HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
				"Bad credentials", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}

	@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex,
			HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse(Instant.now(), HttpStatus.FORBIDDEN.value(), "Forbidden",
				ex.getMessage() != null ? ex.getMessage() : "Access denied", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleFallback(Exception ex, HttpServletRequest request) {
		ErrorResponse error = new ErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
				"An unexpected error occurred", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}
