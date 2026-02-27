package top.hanlin.downloadmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import top.hanlin.downloadmanagement.domain.IpBan;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface IpBanRepository extends JpaRepository<IpBan, Long> {
	@Query("SELECT b FROM IpBan b WHERE b.ip = :ip AND b.active = true AND (b.expiresAt IS NULL OR b.expiresAt > :now) ORDER BY b.createdAt DESC")
	List<IpBan> findActiveBans(String ip, OffsetDateTime now);

	default Optional<IpBan> findActiveBan(String ip, OffsetDateTime now) {
		List<IpBan> bans = findActiveBans(ip, now);
		return bans.isEmpty() ? Optional.empty() : Optional.of(bans.get(0));
	}

	List<IpBan> findByActiveTrueOrderByCreatedAtDesc();

	@Query("SELECT b FROM IpBan b WHERE b.active = true AND b.expiresAt IS NOT NULL AND b.expiresAt <= :now")
	List<IpBan> findExpiredBans(OffsetDateTime now);
}

