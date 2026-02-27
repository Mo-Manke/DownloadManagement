package top.hanlin.downloadmanagement.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import top.hanlin.downloadmanagement.domain.AdminUser;
import top.hanlin.downloadmanagement.repository.AdminUserRepository;
import top.hanlin.downloadmanagement.repository.InstallEventRepository;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class DashboardController {

    private final InstallEventRepository installEventRepository;
    private final AdminUserRepository adminUserRepository;
    private final ObjectMapper objectMapper;

    public DashboardController(InstallEventRepository installEventRepository, AdminUserRepository adminUserRepository, ObjectMapper objectMapper) {
        this.installEventRepository = installEventRepository;
        this.adminUserRepository = adminUserRepository;
        this.objectMapper = objectMapper;
    }

	@GetMapping("/dashboard")
	public String dashboard(@RequestParam(name = "range", defaultValue = "week") String range,
			@RequestParam(name = "user", required = false) Long userId,
			Authentication authentication,
			Model model) {
		AdminUser current = adminUserRepository.findByUsername(authentication.getName()).orElse(null);
		boolean isSuper = current != null && current.getRole() == AdminUser.Role.SUPER_ADMIN;
		Long targetUserId = (isSuper && userId != null) ? userId : (current != null ? current.getId() : null);

		OffsetDateTime end = OffsetDateTime.now(ZoneId.of("Asia/Shanghai"));
		OffsetDateTime start = "month".equalsIgnoreCase(range) ? end.minusDays(30) : end.minusDays(7);
		String fmt = "month".equalsIgnoreCase(range) ? "%Y-%m-%d" : "%Y-%m-%d %H";

		long total = targetUserId == null ? 0 : installEventRepository.countByOwnerId(targetUserId);
		var rows = targetUserId == null ? java.util.List.<Object[]>of() : installEventRepository.aggregateByOwner(targetUserId, start, end, fmt);
		LinkedHashMap<String, Long> series = new LinkedHashMap<>();
		for (Object[] r : rows) {
			series.put(String.valueOf(r[0]), ((Number) r[1]).longValue());
		}

        model.addAttribute("isSuper", isSuper);
		model.addAttribute("total", total);
		model.addAttribute("range", range);
        try {
            String seriesJson = objectMapper.writeValueAsString(series);
            model.addAttribute("seriesJson", seriesJson);
        } catch (Exception e) {
            model.addAttribute("seriesJson", "{}");
        }
        if (isSuper) {
            model.addAttribute("users", adminUserRepository.findAll());
            model.addAttribute("userId", targetUserId);
            try {
                var simple = adminUserRepository.findAll().stream()
                        .map(u -> java.util.Map.of("id", u.getId(), "username", u.getUsername()))
                        .toList();
                model.addAttribute("usersJson", objectMapper.writeValueAsString(simple));
            } catch (Exception e) {
                model.addAttribute("usersJson", "[]");
            }
        } else {
            model.addAttribute("usersJson", "[]");
        }
		if (authentication != null) {
			model.addAttribute("username", authentication.getName());
		}
		return "dashboard";
	}
}


