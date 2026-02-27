package top.hanlin.downloadmanagement.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.hanlin.downloadmanagement.domain.AdminUser;
import top.hanlin.downloadmanagement.repository.AdminUserRepository;

@Controller
@Validated
@RequestMapping("/admin/users")
public class AdminUserController {

	private final AdminUserRepository repository;
	private final PasswordEncoder passwordEncoder;

	public AdminUserController(AdminUserRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public String list(Model model, Authentication authentication) {
		model.addAttribute("users", repository.findAll());
		if (authentication != null) {
			model.addAttribute("username", authentication.getName());
		}
		return "admin-users";
	}

	@PostMapping
	public String create(@RequestParam @NotBlank String username, @RequestParam @NotBlank String password,
			@RequestParam(defaultValue = "ADMIN") String role) {
		AdminUser user = new AdminUser();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(password));
		user.setRole("SUPER_ADMIN".equalsIgnoreCase(role) ? AdminUser.Role.SUPER_ADMIN : AdminUser.Role.ADMIN);
		repository.save(user);
		return "redirect:/admin/users";
	}

	@PostMapping("/{id}/update")
	public String update(@PathVariable Long id, @RequestParam(required = false) String password,
			@RequestParam(required = false) String role, @RequestParam(required = false) Boolean enabled) {
		return repository.findById(id).map(u -> {
			if (password != null && !password.isBlank()) u.setPassword(passwordEncoder.encode(password));
			if (role != null) u.setRole("SUPER_ADMIN".equalsIgnoreCase(role) ? AdminUser.Role.SUPER_ADMIN : AdminUser.Role.ADMIN);
			if (enabled != null) u.setEnabled(enabled);
			repository.save(u);
			return "redirect:/admin/users";
		}).orElse("redirect:/admin/users");
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id) {
		repository.deleteById(id);
		return "redirect:/admin/users";
	}
}


