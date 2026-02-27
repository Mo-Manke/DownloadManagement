package top.hanlin.downloadmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import top.hanlin.downloadmanagement.service.IpStatService;

import java.util.Map;

@RestController
@RequestMapping("/admin/ip/stat")
public class IpStatController {

	private final IpStatService ipStatService;

	public IpStatController(IpStatService ipStatService) {
		this.ipStatService = ipStatService;
	}

	@GetMapping("/ip/{ip}")
	public ResponseEntity<Map<String, Object>> getIpStats(
			@PathVariable String ip,
			@RequestParam(defaultValue = "24h") String range) {
		Map<String, Object> stats = ipStatService.getIpStats(ip, range);
		return ResponseEntity.ok(stats);
	}

	@GetMapping("/global")
	public ResponseEntity<Map<String, Object>> getGlobalStats(
			@RequestParam(defaultValue = "24h") String range) {
		Map<String, Object> stats = ipStatService.getGlobalStats(range);
		return ResponseEntity.ok(stats);
	}
}

