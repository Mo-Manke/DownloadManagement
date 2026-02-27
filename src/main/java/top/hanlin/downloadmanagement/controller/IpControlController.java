package top.hanlin.downloadmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import top.hanlin.downloadmanagement.domain.IpBan;
import top.hanlin.downloadmanagement.domain.IpBlacklist;
import top.hanlin.downloadmanagement.domain.IpControlPath;
import top.hanlin.downloadmanagement.domain.IpWhitelist;
import top.hanlin.downloadmanagement.service.IpControlService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/ip")
public class IpControlController {

	private final IpControlService ipControlService;

	public IpControlController(IpControlService ipControlService) {
		this.ipControlService = ipControlService;
	}

	@GetMapping
	public String index(Authentication auth, Model model) {
		if (auth != null) {
			model.addAttribute("username", auth.getName());
		}
		return "ip-control";
	}

	@GetMapping("/bans")
	@ResponseBody
	public List<IpBan> listBans() {
		return ipControlService.listActiveBans();
	}

	@PostMapping("/ban")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> ban(@RequestBody Map<String, Object> req) {
		String ip = (String) req.get("ip");
		Integer duration = req.get("duration") != null ? (Integer) req.get("duration") : null;
		String description = (String) req.get("description");
		IpBan ban = ipControlService.manualBan(ip, duration, description);
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", true);
		resp.put("ban", ban);
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/unban")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> unban(@RequestBody Map<String, String> req) {
		String ip = req.get("ip");
		ipControlService.unban(ip);
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", true);
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/whitelist")
	@ResponseBody
	public List<IpWhitelist> listWhitelist() {
		return ipControlService.listWhitelist();
	}

	@PostMapping("/whitelist/add")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> addWhitelist(@RequestBody Map<String, String> req) {
		String ip = req.get("ip");
		String description = req.get("description");
		IpWhitelist wl = ipControlService.addWhitelist(ip, description);
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", true);
		resp.put("whitelist", wl);
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/whitelist/remove")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> removeWhitelist(@RequestBody Map<String, String> req) {
		String ip = req.get("ip");
		ipControlService.removeWhitelist(ip);
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", true);
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/blacklist")
	@ResponseBody
	public List<IpBlacklist> listBlacklist() {
		return ipControlService.listBlacklist();
	}

	@PostMapping("/blacklist/add")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> addBlacklist(@RequestBody Map<String, Object> req) {
		String ip = (String) req.get("ip");
		String description = (String) req.get("description");
		Integer duration = req.get("duration") != null ? (Integer) req.get("duration") : null;
		IpBlacklist bl = ipControlService.addBlacklist(ip, description, duration);
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", true);
		resp.put("blacklist", bl);
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/blacklist/remove")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> removeBlacklist(@RequestBody Map<String, String> req) {
		String ip = req.get("ip");
		ipControlService.removeBlacklist(ip);
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", true);
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/stats/{ip}")
	@ResponseBody
	public Map<String, Object> getStats(@PathVariable String ip) {
		return ipControlService.getIpStats(ip);
	}

	@GetMapping("/config")
	@ResponseBody
	public Map<String, String> getConfig() {
		return ipControlService.getAllConfigs();
	}

	@PostMapping("/config")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> req) {
		String key = (String) req.get("key");
		String value = (String) req.get("value");
		String description = (String) req.get("description");
		ipControlService.updateConfig(key, value, description);
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", true);
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/paths")
	@ResponseBody
	public List<IpControlPath> listPaths() {
		return ipControlService.listControlPaths();
	}

	@PostMapping("/paths/add")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> addPath(@RequestBody Map<String, String> req) {
		String path = req.get("path");
		String description = req.get("description");
		IpControlPath controlPath = ipControlService.addControlPath(path, description);
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", true);
		resp.put("path", controlPath);
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/paths/remove")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> removePath(@RequestBody Map<String, Object> req) {
		Long id = req.get("id") instanceof Integer ? ((Integer) req.get("id")).longValue() : (Long) req.get("id");
		ipControlService.removeControlPath(id);
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", true);
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/paths/toggle")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> togglePath(@RequestBody Map<String, Object> req) {
		Long id = req.get("id") instanceof Integer ? ((Integer) req.get("id")).longValue() : (Long) req.get("id");
		Boolean enabled = (Boolean) req.get("enabled");
		ipControlService.toggleControlPath(id, enabled);
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", true);
		return ResponseEntity.ok(resp);
	}
}

