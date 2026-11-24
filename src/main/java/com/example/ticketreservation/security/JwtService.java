package com.example.ticketreservation.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

	private final SecretKey key;
	private final long expirationMs;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generateToken(AppUser user) {
		Claims claims = Jwts.claims().setSubject(user.getUsername());
		claims.put("role", user.getRole().name());
		Date now = new Date();
		Date exp = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(now)
				.setExpiration(exp)
				.signWith(key, SignatureAlgorithm.HS384)
				.compact();
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public Role extractRole(String token) {
		Object role = extractClaim(token, claims -> claims.get("role"));
		return role != null ? Role.valueOf(role.toString()) : null;
	}

	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	public long getExpirationMs() {
		return expirationMs;
	}
}
