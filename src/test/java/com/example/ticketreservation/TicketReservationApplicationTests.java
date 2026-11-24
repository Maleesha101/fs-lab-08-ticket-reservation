package com.example.ticketreservation;

import com.example.ticketreservation.dto.LoginRequest;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;
import com.example.ticketreservation.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	"spring.data.mongodb.uri=mongodb://localhost:27017/ticket_reservation_test_db"
})
class TicketReservationApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@BeforeEach
	void setUp() {
		reservationRepository.deleteAll();
		eventRepository.deleteAll();
		userRepository.deleteAll();

		AppUser admin = new AppUser();
		admin.setUsername("admin");
		admin.setPasswordHash(passwordEncoder.encode("admin123"));
		admin.setFullName("Admin");
		admin.setRole(Role.ADMIN);
		admin.setCreatedAt(Instant.now());
		userRepository.save(admin);

		AppUser customer = new AppUser();
		customer.setUsername("customer");
		customer.setPasswordHash(passwordEncoder.encode("customer123"));
		customer.setFullName("Customer");
		customer.setRole(Role.CUSTOMER);
		customer.setCreatedAt(Instant.now());
		userRepository.save(customer);

		Event event = new Event();
		event.setId("evt_smoke");
		event.setName("Smoke Event");
		event.setDescription("d");
		event.setVenue("V");
		event.setEventDate(LocalDateTime.now().plusDays(5));
		event.setTotalSeats(5);
		event.setAvailableSeats(5);
		event.setTicketPrice(BigDecimal.valueOf(100));
		event.setOrganizerUsername("admin");
		event.setCreatedAt(Instant.now());
		eventRepository.save(event);
	}
	@Test
	void shouldReturn401WithoutToken() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/events")).andReturn();
		assertThat(result.getResponse().getStatus()).isEqualTo(401);
	}

	@Test
	void shouldReturn403ForCustomerCreatingEvent() throws Exception {
		loginAsCustomer();
		MvcResult result = mockMvc.perform(post("/api/events")
						.header("Authorization", "Bearer " + loginAsCustomer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"X\",\"venue\":\"Y\",\"eventDate\":\"2026-12-31T10:00:00\",\"totalSeats\":10,\"ticketPrice\":1.0}"))
				.andReturn();
		assertThat(result.getResponse().getStatus()).isEqualTo(403);
	}

	@Test
	void shouldReturn400ForInvalidBody() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/events")
						.header("Authorization", "Bearer " + loginAsAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\",\"venue\":\"\",\"eventDate\":\"2020-01-01T10:00:00\",\"totalSeats\":0,\"ticketPrice\":-1.0}"))
				.andReturn();
		assertThat(result.getResponse().getStatus()).isEqualTo(400);
	}

	@Test
	void shouldReturn409OnOverbooking() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/reservations")
						.header("Authorization", "Bearer " + loginAsCustomer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":\"evt_smoke\",\"seats\":6}"))
				.andReturn();
		assertThat(result.getResponse().getStatus()).isEqualTo(409);
	}

	@Test
	void customerCannotCancelAnotherUsersReservation() throws Exception {
		String customerToken = loginAsCustomer();
		String resId = bookReservation(customerToken, "evt_smoke", 1).id();

		AppUser otherCustomer = new AppUser();
		otherCustomer.setUsername("customer2");
		otherCustomer.setPasswordHash(passwordEncoder.encode("c123"));
		otherCustomer.setFullName("C2");
		otherCustomer.setRole(Role.CUSTOMER);
		otherCustomer.setCreatedAt(Instant.now());
		userRepository.save(otherCustomer);
		String otherToken = login("customer2", "c123");

		MvcResult result = mockMvc.perform(delete("/api/reservations/" + resId)
						.header("Authorization", "Bearer " + otherToken))
				.andReturn();
		assertThat(result.getResponse().getStatus()).isEqualTo(403);
	}

	@Test
	void jwtTamperedTokenIsRejected() throws Exception {
		String token = loginAsAdmin();
		String tampered = token.substring(0, token.length() - 1) + "x";
		MvcResult result = mockMvc.perform(get("/api/events")
						.header("Authorization", "Bearer " + tampered))
				.andReturn();
		assertThat(result.getResponse().getStatus()).isEqualTo(401);
	}

	@Test
	void concurrentBookingNeverOversells() throws Exception {
		String token = loginAsCustomer();
		int threads = 20;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch latch = new CountDownLatch(threads);
		AtomicInteger success = new AtomicInteger();
		AtomicInteger conflict = new AtomicInteger();

		for (int i = 0; i < threads; i++) {
			executor.submit(() -> {
				try {
					MvcResult r = mockMvc.perform(post("/api/reservations")
									.header("Authorization", "Bearer " + token)
									.contentType(MediaType.APPLICATION_JSON)
									.content("{\"eventId\":\"evt_smoke\",\"seats\":1}"))
							.andReturn();
					int code = r.getResponse().getStatus();
					if (code == 201) {
						success.incrementAndGet();
					} else if (code == 409) {
						conflict.incrementAndGet();
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					latch.countDown();
				}
			});
		}
		latch.await(30, TimeUnit.SECONDS);
		executor.shutdown();

		int total = success.get() + conflict.get();
		assertThat(total).isEqualTo(threads);
		assertThat(success.get()).isEqualTo(5);
		assertThat(conflict.get()).isEqualTo(15);

		Event updated = eventRepository.findById("evt_smoke").orElseThrow();
		assertThat(updated.getAvailableSeats()).isEqualTo(0);
		assertThat(updated.getAvailableSeats()).isNotNegative();
	}

	private String loginAsAdmin() throws Exception {
		return login("admin", "admin123");
	}

	private String loginAsCustomer() throws Exception {
		return login("customer", "customer123");
	}

	private String login(String username, String password) throws Exception {
		LoginRequest lr = new LoginRequest(username, password);
		MvcResult result = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(lr)))
				.andExpect(status().isOk()).andReturn();
		return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
	}

	private com.example.ticketreservation.dto.ReservationResponse bookReservation(String token,
			String eventId, int seats) throws Exception {
		com.example.ticketreservation.dto.ReservationRequest rr = new com.example.ticketreservation.dto.ReservationRequest(eventId,
				seats);
		MvcResult result = mockMvc.perform(post("/api/reservations")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(rr)))
				.andExpect(status().isCreated()).andReturn();
		return mapper.readValue(result.getResponse().getContentAsString(),
				com.example.ticketreservation.dto.ReservationResponse.class);
	}
}