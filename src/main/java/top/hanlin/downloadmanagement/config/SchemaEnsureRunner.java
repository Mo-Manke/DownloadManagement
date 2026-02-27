package top.hanlin.downloadmanagement.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaEnsureRunner {

	@Bean
	public CommandLineRunner ensureRememberMeTable(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS persistent_logins (\n" +
						"username VARCHAR(64) NOT NULL,\n" +
						"series VARCHAR(64) PRIMARY KEY,\n" +
						"token VARCHAR(64) NOT NULL,\n" +
						"last_used TIMESTAMP NOT NULL\n" +
						")");
			} catch (Exception e) {
				System.err.println("警告：persistent_logins表创建失败（可能已存在）: " + e.getMessage());
			}
		};
	}
}


