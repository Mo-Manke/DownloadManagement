package top.hanlin.downloadmanagement.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.hanlin.downloadmanagement.service.PackageService;

import java.io.IOException;

@Controller
@Validated
public class AdminController {

	private final PackageService packageService;

	public AdminController(PackageService packageService) {
		this.packageService = packageService;
	}

    @GetMapping({"/", "/admin"})
    public String admin(Model model, Authentication authentication) {
        var packages = packageService.listFor(authentication);
		model.addAttribute("packages", packages);
		if (authentication != null) {
			model.addAttribute("username", authentication.getName());
			boolean isSuper = authentication.getAuthorities().stream()
					.anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
			model.addAttribute("isSuperAdmin", isSuper);
			if (isSuper) {
				// 为超管准备所有用户信息用于显示所属管理员
				model.addAttribute("allUsers", packageService.getAllUsersMap());
			} else {
				model.addAttribute("allUsers", new java.util.HashMap<>());
			}
		}
		return "admin";
	}

	@PostMapping("/admin/packages")
	public String create(
			@RequestParam @NotBlank String name,
			@RequestParam @NotBlank String version,
			@RequestParam @NotBlank String flag,
			@RequestParam("file") MultipartFile file,
			Model model
	) throws IOException {
		packageService.create(name, version, flag, file);
		return "redirect:/admin";
	}

    @PostMapping("/admin/packages/{id}/toggle")
	public String toggle(@PathVariable Long id) {
		packageService.toggleEnabled(id);
		return "redirect:/admin";
	}

	@PostMapping("/admin/packages/{id}/delete")
	public String delete(@PathVariable Long id) {
		packageService.deleteById(id);
		return "redirect:/admin";
	}

	@PostMapping("/admin/packages/{id}/update")
	public String update(
			@PathVariable Long id,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String version,
			@RequestParam(required = false) String flag,
			@RequestParam(required = false, name = "filePath") String filePath
	) {
		packageService.updatePackage(id, name, version, flag, filePath);
		return "redirect:/admin";
	}
}


