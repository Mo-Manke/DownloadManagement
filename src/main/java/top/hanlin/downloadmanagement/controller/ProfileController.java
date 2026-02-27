package top.hanlin.downloadmanagement.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.hanlin.downloadmanagement.domain.AccountRequest;
import top.hanlin.downloadmanagement.domain.AdminUser;
import top.hanlin.downloadmanagement.repository.AccountRequestRepository;
import top.hanlin.downloadmanagement.repository.AdminUserRepository;

@Controller
@Validated
public class ProfileController {

	private final AdminUserRepository userRepository;
	private final AccountRequestRepository requestRepository;
	private final PasswordEncoder encoder;

	public ProfileController(AdminUserRepository userRepository, AccountRequestRepository requestRepository, PasswordEncoder encoder) {
		this.userRepository = userRepository;
		this.requestRepository = requestRepository;
		this.encoder = encoder;
	}

	@GetMapping("/profile")
	public String profile(Authentication auth, Model model) {
		AdminUser me = userRepository.findByUsername(auth.getName()).orElse(null);
		model.addAttribute("me", me);
		if (auth != null) {
			model.addAttribute("username", auth.getName());
			boolean isSuper = me != null && me.getRole() == AdminUser.Role.SUPER_ADMIN;
			model.addAttribute("isSuperAdmin", isSuper);
		}
		return "profile";
	}

	@PostMapping("/profile/password")
	public String changePassword(Authentication auth, @RequestParam @NotBlank String password, jakarta.servlet.http.HttpServletRequest request) {
		userRepository.findByUsername(auth.getName()).ifPresent(u -> {
			u.setPassword(encoder.encode(password));
			userRepository.save(u);
		});
		// 修改密码后登出并跳转登录页
		try {
			jakarta.servlet.http.HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
		} catch (Exception ignored) {}
		return "redirect:/login?changed";
	}

	@PostMapping("/profile/username")
	public String changeUsername(Authentication auth, @RequestParam @NotBlank String username) {
		AdminUser me = userRepository.findByUsername(auth.getName()).orElse(null);
		if (me == null) return "redirect:/profile";
		if (me.getRole() == AdminUser.Role.SUPER_ADMIN) {
			// 超级管理员可直接修改，但需要确认（前端已处理）
			if (userRepository.findByUsername(username).isPresent() && !username.equals(me.getUsername())) {
				// 用户名已存在且不是自己
				return "redirect:/profile?error=username_exists";
			}
			me.setUsername(username);
			userRepository.save(me);
		} else {
			AccountRequest req = new AccountRequest();
			req.setType(AccountRequest.Type.USERNAME_CHANGE);
			req.setRequesterId(me.getId());
			req.setTargetUserId(me.getId());
			req.setNewUsername(username);
			requestRepository.save(req);
		}
		return "redirect:/profile";
	}

	@PostMapping("/auth/forgot")
	public String forgot(@RequestParam @NotBlank String username) {
		userRepository.findByUsername(username).ifPresent(u -> {
			AccountRequest req = new AccountRequest();
			req.setType(AccountRequest.Type.PASSWORD_RESET);
			req.setRequesterId(u.getId());
			req.setTargetUserId(u.getId());
			requestRepository.save(req);
		});
		return "redirect:/login";
	}
}


