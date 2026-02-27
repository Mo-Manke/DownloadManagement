package top.hanlin.downloadmanagement.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "top.hanlin.downloadmanagement.domain")
@EnableJpaRepositories(basePackages = "top.hanlin.downloadmanagement.repository")
public class JpaConfig {
}


