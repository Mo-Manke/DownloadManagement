package top.hanlin.downloadmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.hanlin.downloadmanagement.domain.AppPackage;

import java.util.Optional;

public interface AppPackageRepository extends JpaRepository<AppPackage, Long> {
	Optional<AppPackage> findByFlagAndEnabledTrue(String flag);
	java.util.List<AppPackage> findByFlag(String flag);
	java.util.List<AppPackage> findByFlagAndIdNot(String flag, Long id);
	Optional<AppPackage> findByOwnerIdAndFlagAndEnabledTrue(Long ownerId, String flag);
	java.util.List<AppPackage> findByOwnerId(Long ownerId);
}


