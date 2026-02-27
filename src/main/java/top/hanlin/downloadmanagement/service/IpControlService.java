package top.hanlin.downloadmanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.hanlin.downloadmanagement.domain.*;
import top.hanlin.downloadmanagement.repository.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IpControlService {

	private final IpBanRepository banRepo;
	private final IpWhitelistRepository whitelistRepo;
	private final IpBlacklistRepository blacklistRepo;
	private final AccessLogRepository accessLogRepo;
	private final IpControlConfigRepository configRepo;
	private final IpControlPathRepository pathRepo;

	@Value("${app.ip.rateLimit.perMinute:60}")
	private int defaultPerMinuteLimit;

	@Value("${app.ip.concurrentLimit:10}")
	private int defaultConcurrentLimit;

	@Value("${app.ip.downloadRateLimitKB:1024}")
	private int defaultDownloadRateLimitKB;

	@Value("${app.ip.autoBanOnExceed:true}")
	private boolean defaultAutoBanOnExceed;

	@Value("${app.ip.autoBanDurationMinutes:60}")
	private int defaultAutoBanDurationMinutes;

	// 内存中的并发连接计数（生产环境建议用Redis）
	private final Map<String, Integer> concurrentConnections = new ConcurrentHashMap<>();
	private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();
	private final Map<String, Integer> accessCounts = new ConcurrentHashMap<>();

	public IpControlService(IpBanRepository banRepo, IpWhitelistRepository whitelistRepo,
			IpBlacklistRepository blacklistRepo, AccessLogRepository accessLogRepo,
			IpControlConfigRepository configRepo, IpControlPathRepository pathRepo) {
		this.banRepo = banRepo;
		this.whitelistRepo = whitelistRepo;
		this.blacklistRepo = blacklistRepo;
		this.accessLogRepo = accessLogRepo;
		this.configRepo = configRepo;
		this.pathRepo = pathRepo;
	}

	public boolean shouldApplyIpControl(String path) {
		List<IpControlPath> enabledPaths = pathRepo.findByEnabledTrue();
		if (enabledPaths.isEmpty()) {
			// 如果没有配置路径，默认只对 /d/ 生效
			return path.startsWith("/d/");
		}
		// 检查路径是否匹配任何启用的路径配置
		for (IpControlPath controlPath : enabledPaths) {
			String configuredPath = controlPath.getPath();
			if (path.startsWith(configuredPath)) {
				return true;
			}
		}
		return false;
	}

	private int getPerMinuteLimit() {
		return configRepo.findByConfigKey("perMinuteLimit")
				.map(c -> Integer.parseInt(c.getConfigValue()))
				.orElse(defaultPerMinuteLimit);
	}

	private int getConcurrentLimit() {
		return configRepo.findByConfigKey("concurrentLimit")
				.map(c -> Integer.parseInt(c.getConfigValue()))
				.orElse(defaultConcurrentLimit);
	}

	private int getDownloadRateLimitKB() {
		return configRepo.findByConfigKey("downloadRateLimitKB")
				.map(c -> Integer.parseInt(c.getConfigValue()))
				.orElse(defaultDownloadRateLimitKB);
	}

	private boolean getAutoBanOnExceed() {
		return configRepo.findByConfigKey("autoBanOnExceed")
				.map(c -> Boolean.parseBoolean(c.getConfigValue()))
				.orElse(defaultAutoBanOnExceed);
	}

	private int getAutoBanDurationMinutes() {
		return configRepo.findByConfigKey("autoBanDurationMinutes")
				.map(c -> Integer.parseInt(c.getConfigValue()))
				.orElse(defaultAutoBanDurationMinutes);
	}

	public boolean isWhitelisted(String ip) {
		return whitelistRepo.findByIp(ip).isPresent();
	}

	public boolean isBlacklisted(String ip) {
		return blacklistRepo.findActiveBlacklist(ip, OffsetDateTime.now()).isPresent();
	}

	public boolean isBanned(String ip) {
		if (isWhitelisted(ip)) return false;
		return banRepo.findActiveBan(ip, OffsetDateTime.now()).isPresent();
	}

	@Transactional
	public void recordAccess(String ip, String path, Long bytes) {
		AccessLog log = new AccessLog();
		log.setIp(ip);
		log.setPath(path);
		log.setBytesTransferred(bytes);
		accessLogRepo.save(log);
	}

	public boolean checkRateLimit(String ip) {
		if (isWhitelisted(ip)) return true;
		long now = System.currentTimeMillis();
		long windowStart = now - 60000; // 1分钟窗口
		OffsetDateTime since = OffsetDateTime.now().minusMinutes(1);
		long count = accessLogRepo.countByIpSince(ip, since);
		int limit = getPerMinuteLimit();
		if (count >= limit) {
			if (getAutoBanOnExceed()) {
				autoBan(ip, IpBan.BanReason.RATE_LIMIT_EXCEEDED, "每分钟访问超过" + limit + "次");
			}
			return false;
		}
		return true;
	}

	public boolean checkConcurrentLimit(String ip) {
		if (isWhitelisted(ip)) return true;
		int current = concurrentConnections.getOrDefault(ip, 0);
		int limit = getConcurrentLimit();
		if (current >= limit) {
			if (getAutoBanOnExceed()) {
				autoBan(ip, IpBan.BanReason.CONCURRENT_EXCEEDED, "并发连接数超过" + limit);
			}
			return false;
		}
		return true;
	}

	public void incrementConcurrent(String ip) {
		concurrentConnections.merge(ip, 1, Integer::sum);
	}

	public void decrementConcurrent(String ip) {
		concurrentConnections.computeIfPresent(ip, (k, v) -> v > 1 ? v - 1 : null);
	}

	public int getDownloadRateLimitKB(String ip) {
		if (isWhitelisted(ip)) return Integer.MAX_VALUE;
		return getDownloadRateLimitKB();
	}

	@Transactional
	public void autoBan(String ip, IpBan.BanReason reason, String description) {
		if (isWhitelisted(ip) || isBanned(ip)) return;
		IpBan ban = new IpBan();
		ban.setIp(ip);
		ban.setBanType(IpBan.BanType.AUTO);
		ban.setReason(reason);
		ban.setDescription(description);
		ban.setExpiresAt(OffsetDateTime.now().plusMinutes(getAutoBanDurationMinutes()));
		ban.setActive(true);
		banRepo.save(ban);
	}

	@Transactional
	public IpBan manualBan(String ip, Integer durationMinutes, String description) {
		IpBan ban = new IpBan();
		ban.setIp(ip);
		ban.setBanType(IpBan.BanType.MANUAL);
		ban.setReason(IpBan.BanReason.MANUAL_BAN);
		ban.setDescription(description);
		ban.setExpiresAt(durationMinutes != null ? OffsetDateTime.now().plusMinutes(durationMinutes) : null);
		ban.setActive(true);
		return banRepo.save(ban);
	}

	@Transactional
	public void unban(String ip) {
		List<IpBan> activeBans = banRepo.findActiveBans(ip, OffsetDateTime.now());
		for (IpBan ban : activeBans) {
			ban.setActive(false);
			banRepo.save(ban);
		}
	}

	public List<IpBan> listActiveBans() {
		return banRepo.findByActiveTrueOrderByCreatedAtDesc();
	}

	@Transactional
	public void cleanupExpiredBans() {
		banRepo.findExpiredBans(OffsetDateTime.now()).forEach(b -> {
			b.setActive(false);
			banRepo.save(b);
		});
		blacklistRepo.findExpiredBlacklists(OffsetDateTime.now()).forEach(blacklistRepo::delete);
	}

	public Map<String, Object> getIpStats(String ip) {
		OffsetDateTime since = OffsetDateTime.now().minusMinutes(1);
		long count = accessLogRepo.countByIpSince(ip, since);
		Long bytes = accessLogRepo.sumBytesByIpSince(ip, since);
		Map<String, Object> stats = new java.util.HashMap<>();
		stats.put("accessCountLastMinute", count);
		stats.put("bytesTransferredLastMinute", bytes != null ? bytes : 0);
		stats.put("concurrentConnections", concurrentConnections.getOrDefault(ip, 0));
		stats.put("isWhitelisted", isWhitelisted(ip));
		stats.put("isBlacklisted", isBlacklisted(ip));
		stats.put("isBanned", isBanned(ip));
		return stats;
	}

	@Transactional
	public IpWhitelist addWhitelist(String ip, String description) {
		Optional<IpWhitelist> existing = whitelistRepo.findByIp(ip);
		if (existing.isPresent()) {
			return existing.get();
		}
		IpWhitelist wl = new IpWhitelist();
		wl.setIp(ip);
		wl.setDescription(description);
		return whitelistRepo.save(wl);
	}

	@Transactional
	public void removeWhitelist(String ip) {
		whitelistRepo.findByIp(ip).ifPresent(whitelistRepo::delete);
	}

	public List<IpWhitelist> listWhitelist() {
		return whitelistRepo.findAll();
	}

	@Transactional
	public IpBlacklist addBlacklist(String ip, String description, Integer durationMinutes) {
		Optional<IpBlacklist> existing = blacklistRepo.findActiveBlacklist(ip, OffsetDateTime.now());
		if (existing.isPresent()) {
			return existing.get();
		}
		IpBlacklist bl = new IpBlacklist();
		bl.setIp(ip);
		bl.setDescription(description);
		bl.setExpiresAt(durationMinutes != null ? OffsetDateTime.now().plusMinutes(durationMinutes) : null);
		return blacklistRepo.save(bl);
	}

	@Transactional
	public void removeBlacklist(String ip) {
		blacklistRepo.findActiveBlacklist(ip, OffsetDateTime.now()).ifPresent(blacklistRepo::delete);
	}

	public List<IpBlacklist> listBlacklist() {
		return blacklistRepo.findAllByOrderByCreatedAtDesc();
	}

	public List<IpControlPath> listControlPaths() {
		return pathRepo.findAll();
	}

	@Transactional
	public IpControlPath addControlPath(String path, String description) {
		IpControlPath controlPath = new IpControlPath();
		controlPath.setPath(path);
		controlPath.setDescription(description);
		controlPath.setEnabled(true);
		return pathRepo.save(controlPath);
	}

	@Transactional
	public void removeControlPath(Long id) {
		pathRepo.deleteById(id);
	}

	@Transactional
	public void toggleControlPath(Long id, boolean enabled) {
		pathRepo.findById(id).ifPresent(path -> {
			path.setEnabled(enabled);
			pathRepo.save(path);
		});
	}

	@Transactional
	public void updateConfig(String key, String value, String description) {
		Optional<IpControlConfig> existing = configRepo.findByConfigKey(key);
		IpControlConfig config;
		if (existing.isPresent()) {
			config = existing.get();
			config.setConfigValue(value);
			if (description != null) {
				config.setDescription(description);
			}
		} else {
			config = new IpControlConfig();
			config.setConfigKey(key);
			config.setConfigValue(value);
			config.setDescription(description);
		}
		configRepo.save(config);
	}

	public Map<String, String> getAllConfigs() {
		Map<String, String> configs = new java.util.HashMap<>();
		configs.put("perMinuteLimit", String.valueOf(getPerMinuteLimit()));
		configs.put("concurrentLimit", String.valueOf(getConcurrentLimit()));
		configs.put("downloadRateLimitKB", String.valueOf(getDownloadRateLimitKB()));
		configs.put("autoBanOnExceed", String.valueOf(getAutoBanOnExceed()));
		configs.put("autoBanDurationMinutes", String.valueOf(getAutoBanDurationMinutes()));
		return configs;
	}
}

