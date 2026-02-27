package top.hanlin.downloadmanagement.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.hanlin.downloadmanagement.domain.IpControlPath;
import top.hanlin.downloadmanagement.repository.IpControlPathRepository;

@Configuration
public class BootstrapControlPath {

	@Bean
	public CommandLineRunner createDefaultControlPath(IpControlPathRepository repository) {
		return args -> {
			if (repository.count() == 0) {
				IpControlPath path = new IpControlPath();
				path.setPath("/d/");
				path.setDescription("默认下载路径");
				path.setEnabled(true);
				repository.save(path);
			}
		};
	}
}

