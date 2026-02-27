package top.hanlin.downloadmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import top.hanlin.downloadmanagement.domain.IpBlacklist;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, Long> {
	@Query("SELECT b FROM IpBlacklist b WHERE b.ip = :ip AND (b.expiresAt IS NULL OR b.expiresAt > :now)")
	Optional<IpBlacklist> findActiveBlacklist(String ip, OffsetDateTime now);

	List<IpBlacklist> findAllByOrderByCreatedAtDesc();

	@Query("SELECT b FROM IpBlacklist b WHERE b.expiresAt IS NOT NULL AND b.expiresAt <= :now")
	List<IpBlacklist> findExpiredBlacklists(OffsetDateTime now);
}

