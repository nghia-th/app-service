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
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
		user.setCreatedBy("test");
		user.setUpdatedBy("test");
		user.setDeleted(false);

		UserEntity saved = userRepository.save(user);
		assertNotNull(saved.getId());

		UserEntity found = userRepository.findById(saved.getId());
		assertNotNull(found);
		assertEquals("integration_test_user", found.getUsername());
		assertEquals("test@example.com", found.getEmail());

		userRepository.deleteById(saved.getId());
		assertNull(userRepository.findById(saved.getId()));
	}
}
