package top.hanlin.downloadmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import top.hanlin.downloadmanagement.domain.AccessLog;

import java.time.OffsetDateTime;
import java.util.List;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
	@Query("SELECT COUNT(a) FROM AccessLog a WHERE a.ip = :ip AND a.occurredAt >= :since")
	long countByIpSince(String ip, OffsetDateTime since);

	@Query("SELECT SUM(a.bytesTransferred) FROM AccessLog a WHERE a.ip = :ip AND a.occurredAt >= :since AND a.bytesTransferred IS NOT NULL")
	Long sumBytesByIpSince(String ip, OffsetDateTime since);
}

