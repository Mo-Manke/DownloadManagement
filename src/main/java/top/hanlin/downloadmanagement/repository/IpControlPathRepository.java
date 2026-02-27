package top.hanlin.downloadmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.hanlin.downloadmanagement.domain.IpControlPath;

import java.util.List;

public interface IpControlPathRepository extends JpaRepository<IpControlPath, Long> {
	List<IpControlPath> findByEnabledTrue();
}

