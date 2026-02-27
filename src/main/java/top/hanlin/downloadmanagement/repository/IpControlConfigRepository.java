package top.hanlin.downloadmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.hanlin.downloadmanagement.domain.IpControlConfig;

import java.util.Optional;

public interface IpControlConfigRepository extends JpaRepository<IpControlConfig, Long> {
	Optional<IpControlConfig> findByConfigKey(String key);
}

