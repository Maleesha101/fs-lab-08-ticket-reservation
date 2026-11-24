package com.example.ticketreservation.security;

import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.UserRepository;
import com.example.ticketreservation.security.ErrorResponseWriter;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final ErrorResponseWriter errorResponseWriter;

	public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
			ErrorResponseWriter errorResponseWriter) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.errorResponseWriter = errorResponseWriter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		final String path = request.getRequestURI();
		final String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		final String token = authHeader.substring(7);
		try {
			final String username = jwtService.extractUsername(token);
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails user = userDetailsService.loadUserByUsername(username);
				if (jwtService.isTokenValid(token, user)) {
					Role role = jwtService.extractRole(token);
					List<SimpleGrantedAuthority> authorities = List
							.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null,
							authorities);
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		} catch (ExpiredJwtException e) {
			// Token expired — let the entry point answer 401 JSON
			errorResponseWriter.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
					"Token expired");
			return;
		} catch (Exception e) {
			// Invalid / tampered token — let the entry point answer 401 JSON
			errorResponseWriter.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
					"Invalid token");
			return;
		}
		filterChain.doFilter(request, response);
	}
}