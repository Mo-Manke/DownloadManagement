package top.hanlin.downloadmanagement.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.hanlin.downloadmanagement.domain.AdminUser;
import top.hanlin.downloadmanagement.repository.AdminUserRepository;

@Configuration
public class BootstrapAdmin {

	@Bean
	public CommandLineRunner createDefaultSuperAdmin(AdminUserRepository repository, PasswordEncoder encoder) {
		return args -> {
			if (repository.count() == 0) {
				AdminUser u = new AdminUser();
				u.setUsername("admin");
				u.setPassword(encoder.encode("admin123"));
				u.setRole(AdminUser.Role.SUPER_ADMIN);
				u.setEnabled(true);
				repository.save(u);
			}
		};
	}
}


