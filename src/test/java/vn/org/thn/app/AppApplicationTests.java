package vn.org.thn.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.org.thn.app.modules.user.domain.entity.UserEntity;
import vn.org.thn.app.modules.user.infrastructure.UserRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AppApplicationTests {

	@Autowired
	private UserRepository userRepository;

	@Test
	void contextLoads() {
		assertNotNull(userRepository);
	}

	@Test
	void testUserCrudIntegration() {
		UserEntity user = new UserEntity();
		user.setUsername("integration_test_user");
		user.setEmail("test@example.com");
		user.setFullName("Integration Test");
		user.setRole("USER");
		user.setStatus("ACTIVE");

		UserEntity saved = userRepository.save(user);
		assertNotNull(saved.getId());
		assertEquals("integration test", saved.getFullNameUnaccent(), "fullNameUnaccent should be auto-populated");
		assertNotNull(saved.getCreatedAt(), "createdAt should be auto-populated");
		assertNotNull(saved.getCreatedBy(), "createdBy should be auto-populated");

		UserEntity found = userRepository.findById(saved.getId());
		assertNotNull(found);
		assertEquals("integration_test_user", found.getUsername());
		assertEquals("test@example.com", found.getEmail());
		assertEquals("integration test", found.getFullNameUnaccent());

		vn.org.thn.app.modules.user.api.dto.UserResponse response = vn.org.thn.app.modules.user.api.dto.UserResponse.fromEntity(found);
		assertNotNull(response.getCreatedAt());
		assertNotNull(response.getCreatedBy());

		userRepository.deleteById(saved.getId());
		assertNull(userRepository.findById(saved.getId()));
	}
}
