package top.hanlin.downloadmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.hanlin.downloadmanagement.domain.InstallEvent;

import java.time.OffsetDateTime;
import java.util.List;

public interface InstallEventRepository extends JpaRepository<InstallEvent, Long> {

	long countByOwnerId(Long ownerId);
	long countByPackageId(Long packageId);
	long countByOwnerIdAndOccurredAtBetween(Long ownerId, OffsetDateTime start, OffsetDateTime end);

	@Query(value = "SELECT DATE_FORMAT(CONVERT_TZ(e.occurred_at, '+00:00', '+08:00'), :fmt) as bucket, COUNT(e.id) as c " +
			"FROM install_event e WHERE e.owner_id = :ownerId AND e.occurred_at BETWEEN :start AND :end " +
			"GROUP BY DATE_FORMAT(CONVERT_TZ(e.occurred_at, '+00:00', '+08:00'), :fmt) ORDER BY bucket", nativeQuery = true)
	List<Object[]> aggregateByOwner(@Param("ownerId") Long ownerId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end, @Param("fmt") String fmt);
}


