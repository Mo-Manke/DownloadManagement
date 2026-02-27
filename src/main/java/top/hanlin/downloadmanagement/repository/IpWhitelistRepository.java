package top.hanlin.downloadmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.hanlin.downloadmanagement.domain.IpWhitelist;

import java.util.Optional;

public interface IpWhitelistRepository extends JpaRepository<IpWhitelist, Long> {
	Optional<IpWhitelist> findByIp(String ip);
}

