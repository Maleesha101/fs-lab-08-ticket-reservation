package com.example.ticketreservation.security;

import com.example.ticketreservation.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import com.example.ticketreservation.model.AppUser;

import java.util.stream.Stream;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser user = userRepository.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("User not found: " + username);
		}
		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getUsername())
				.password(user.getPasswordHash())
				.roles(user.getRole().name())
				.accountExpired(false)
				.accountLocked(false)
				.credentialsExpired(false)
				.disabled(false)
				.build();
	}
}
