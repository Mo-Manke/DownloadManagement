package top.hanlin.downloadmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.hanlin.downloadmanagement.domain.AppPackage;
import top.hanlin.downloadmanagement.repository.AdminUserRepository;
import top.hanlin.downloadmanagement.controller.dto.PackageView;
import top.hanlin.downloadmanagement.service.PackageService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/packages")
public class ApiPackageController {

    private final PackageService packageService;
    private final AdminUserRepository adminUserRepository;

    public ApiPackageController(PackageService packageService, AdminUserRepository adminUserRepository) {
        this.packageService = packageService;
        this.adminUserRepository = adminUserRepository;
    }

    @GetMapping
    public List<PackageView> list(org.springframework.security.core.Authentication authentication) {
        var list = packageService.listFor(authentication);
        return list.stream().map(p -> PackageView.of(
                p.getId(), p.getName(), p.getVersion(), p.getFlag(), p.isEnabled(), p.getOwnerId(),
                adminUserRepository.findById(p.getOwnerId()).map(u -> u.getUsername()).orElse(""))
        ).toList();
    }

	@PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> create(
			@RequestParam String name,
			@RequestParam String version,
			@RequestParam String flag,
			@RequestParam("file") MultipartFile file
	) throws IOException {
        AppPackage p = packageService.create(name, version, flag, file);
        String owner = adminUserRepository.findById(p.getOwnerId()).map(u -> u.getUsername()).orElse("");
        return ResponseEntity.ok(PackageView.of(p.getId(), p.getName(), p.getVersion(), p.getFlag(), p.isEnabled(), p.getOwnerId(), owner));
	}

	@PostMapping("/{id}/toggle")
	public ResponseEntity<?> toggle(@PathVariable Long id) {
		packageService.toggleEnabled(id);
		Map<String, Object> resp = new HashMap<>();
		resp.put("ok", true);
		return ResponseEntity.ok(resp);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Long id) {
		packageService.deleteById(id);
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/{id}")
	public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
		packageService.updatePackage(id,
				body.getOrDefault("name", null),
				body.getOrDefault("version", null),
				body.getOrDefault("flag", null),
				body.getOrDefault("filePath", null));
		return ResponseEntity.ok().build();
	}
}


