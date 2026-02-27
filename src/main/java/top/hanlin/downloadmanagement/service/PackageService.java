package top.hanlin.downloadmanagement.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.hanlin.downloadmanagement.domain.AppPackage;
import top.hanlin.downloadmanagement.domain.AdminUser;
import top.hanlin.downloadmanagement.repository.AppPackageRepository;
import top.hanlin.downloadmanagement.repository.AdminUserRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PackageService {

	private final AppPackageRepository repository;
    private final StorageService storageService;
    private final AdminUserRepository adminUserRepository;

    public PackageService(AppPackageRepository repository, StorageService storageService, AdminUserRepository adminUserRepository) {
        this.repository = repository;
        this.storageService = storageService;
        this.adminUserRepository = adminUserRepository;
    }

	public List<AppPackage> listAll() {
		return repository.findAll();
	}

	@Transactional
    public AppPackage create(String name, String version, String flag, MultipartFile file) throws IOException {
		String path = storageService.storeApk(file);
		AppPackage p = new AppPackage();
		p.setName(name);
		p.setVersion(version);
		p.setFlag(flag);
		p.setFilePath(path);
		p.setEnabled(true);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            adminUserRepository.findByUsername(auth.getName()).ifPresent(u -> p.setOwnerId(u.getId()));
        }
		return repository.save(p);
	}

	@Transactional
	public void toggleEnabled(Long id) {
        repository.findById(id).ifPresent(p -> {
            boolean toEnabled = !p.isEnabled();
            p.setEnabled(toEnabled);
            repository.save(p);
            if (toEnabled) {
                for (var other : repository.findByFlagAndIdNot(p.getFlag(), p.getId())) {
                    if (other.isEnabled()) { other.setEnabled(false); repository.save(other); }
                }
            }
        });
	}

    @Transactional
    public void deleteById(Long id) {
        repository.findById(id).ifPresent(p -> {
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(p.getFilePath())); } catch (Exception ignored) {}
            repository.deleteById(id);
        });
    }

	@Transactional
	public void updatePackage(Long id, String name, String version, String flag, String filePath) {
		repository.findById(id).ifPresent(p -> {
			if (name != null && !name.isBlank()) p.setName(name);
			if (version != null && !version.isBlank()) p.setVersion(version);
			if (flag != null && !flag.isBlank()) p.setFlag(flag);
			if (filePath != null && !filePath.isBlank()) p.setFilePath(filePath);
			repository.save(p);
		});
	}

	public Optional<AppPackage> findEnabledByFlag(String flag) {
		return repository.findByFlagAndEnabledTrue(flag);
	}

	public Optional<AppPackage> findEnabledByOwnerAndFlag(String username, String flag) {
		return adminUserRepository.findByUsername(username)
			.flatMap(u -> repository.findByOwnerIdAndFlagAndEnabledTrue(u.getId(), flag));
	}

	public Map<Long, String> getAllUsersMap() {
		return adminUserRepository.findAll().stream()
			.collect(Collectors.toMap(AdminUser::getId, AdminUser::getUsername));
	}

	public List<AppPackage> listFor(Authentication authentication) {
		if (authentication == null) return List.of();
		boolean isSuper = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
		if (isSuper) return repository.findAll();
		return adminUserRepository.findByUsername(authentication.getName())
			.map(u -> repository.findByOwnerId(u.getId()))
			.orElse(List.of());
	}
}


