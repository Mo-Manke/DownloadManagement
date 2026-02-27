package top.hanlin.downloadmanagement.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import top.hanlin.downloadmanagement.domain.AccountRequest;
import top.hanlin.downloadmanagement.domain.AdminUser;
import top.hanlin.downloadmanagement.repository.AccountRequestRepository;
import top.hanlin.downloadmanagement.repository.AdminUserRepository;

@Controller
@RequestMapping("/admin/requests")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AccountApproveController {

	private final AccountRequestRepository reqRepo;
	private final AdminUserRepository userRepo;
	private final PasswordEncoder encoder;

	public AccountApproveController(AccountRequestRepository reqRepo, AdminUserRepository userRepo, PasswordEncoder encoder) {
		this.reqRepo = reqRepo;
		this.userRepo = userRepo;
		this.encoder = encoder;
	}

	@GetMapping
	public String list(Model model, Authentication authentication) {
		model.addAttribute("requests", reqRepo.findByStatusOrderByCreatedAtAsc(AccountRequest.Status.PENDING));
		if (authentication != null) {
			model.addAttribute("username", authentication.getName());
		}
		return "admin-requests";
	}

	@PostMapping("/{id}/approve")
	public String approve(@PathVariable Long id) {
		return reqRepo.findById(id).map(r -> {
			userRepo.findById(r.getTargetUserId()).ifPresent(u -> {
				if (r.getType() == AccountRequest.Type.USERNAME_CHANGE && r.getNewUsername() != null) {
					u.setUsername(r.getNewUsername());
				}
				if (r.getType() == AccountRequest.Type.PASSWORD_RESET && r.getNewPasswordHash() != null) {
					u.setPassword(r.getNewPasswordHash());
				}
				userRepo.save(u);
			});
			r.setStatus(AccountRequest.Status.APPROVED);
			reqRepo.save(r);
			return "redirect:/admin/requests";
		}).orElse("redirect:/admin/requests");
	}

	@PostMapping("/{id}/reject")
	public String reject(@PathVariable Long id) {
		reqRepo.findById(id).ifPresent(r -> { r.setStatus(AccountRequest.Status.REJECTED); reqRepo.save(r); });
		return "redirect:/admin/requests";
	}

	@PostMapping("/{id}/set-password")
	public String setPassword(@PathVariable Long id, @RequestParam String password) {
		return reqRepo.findById(id).map(r -> {
			if (r.getType() == AccountRequest.Type.PASSWORD_RESET) {
				userRepo.findById(r.getTargetUserId()).ifPresent(u -> {
					u.setPassword(encoder.encode(password));
					userRepo.save(u);
				});
				r.setStatus(AccountRequest.Status.APPROVED);
				reqRepo.save(r);
			}
			return "redirect:/admin/requests";
		}).orElse("redirect:/admin/requests");
	}
}


