package top.hanlin.downloadmanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.hanlin.downloadmanagement.domain.IpAccessStat;
import top.hanlin.downloadmanagement.repository.IpAccessStatRepository;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class IpStatService {

	private final IpAccessStatRepository statRepo;
	private final String serverId;

	// 批量写入缓存
	private final List<IpAccessStat> batchCache = Collections.synchronizedList(new ArrayList<>());
	private static final int BATCH_SIZE = 100;

	public IpStatService(IpAccessStatRepository statRepo, 
			@Value("${app.server.id:server-${random.value}}") String serverId) {
		this.statRepo = statRepo;
		this.serverId = serverId;
	}

	@Async
	public CompletableFuture<Void> recordAccess(String ip, Long downloadBytes) {
		IpAccessStat stat = new IpAccessStat();
		stat.setIp(ip);
		stat.setOccurredAt(OffsetDateTime.now());
		stat.setRequestCount(1L);
		stat.setDownloadBytes(downloadBytes != null ? downloadBytes : 0L);
		stat.setServerId(serverId);

		synchronized (batchCache) {
			batchCache.add(stat);
			if (batchCache.size() >= BATCH_SIZE) {
				flushBatch();
			}
		}
		return CompletableFuture.completedFuture(null);
	}

	@Transactional
	public void flushBatch() {
		if (!batchCache.isEmpty()) {
			statRepo.saveAll(new ArrayList<>(batchCache));
			batchCache.clear();
		}
	}

	public Map<String, Object> getIpStats(String ip, String timeRange) {
		OffsetDateTime startTime = getStartTime(timeRange);
		Map<String, Object> result = new HashMap<>();

		// 总统计
		Map<String, Object> totalStats = statRepo.getIpTotalStats(ip, startTime);
		result.put("totalRequests", totalStats.get("totalRequests") != null ? totalStats.get("totalRequests") : 0);
		result.put("totalBytes", totalStats.get("totalBytes") != null ? totalStats.get("totalBytes") : 0);

		// 按小时分布
		List<Map<String, Object>> hourlyStats = statRepo.getIpStatsByHour(ip, startTime);
		result.put("hourlyDistribution", hourlyStats);

		// 按分钟分布（仅最近1小时）
		if ("1h".equals(timeRange)) {
			List<Map<String, Object>> minuteStats = statRepo.getIpStatsByMinute(ip, startTime);
			result.put("minuteDistribution", minuteStats);
		}

		return result;
	}

	public Map<String, Object> getGlobalStats(String timeRange) {
		OffsetDateTime startTime = getStartTime(timeRange);
		Map<String, Object> result = new HashMap<>();

		// 总统计
		Map<String, Object> totalStats = statRepo.getGlobalTotalStats(startTime);
		result.put("totalRequests", totalStats.get("totalRequests") != null ? totalStats.get("totalRequests") : 0);
		result.put("totalBytes", totalStats.get("totalBytes") != null ? totalStats.get("totalBytes") : 0);

		// 按时间段分布
		if ("1h".equals(timeRange) || "24h".equals(timeRange)) {
			List<Map<String, Object>> hourlyStats = statRepo.getGlobalStatsByHour(startTime);
			result.put("timeDistribution", hourlyStats);
		} else {
			List<Map<String, Object>> dailyStats = statRepo.getGlobalStatsByDay(startTime);
			result.put("timeDistribution", dailyStats);
		}

		// Top IPs
		List<Map<String, Object>> topIps = statRepo.getTopIpsByRequests(startTime);
		result.put("topIps", topIps.size() > 10 ? topIps.subList(0, 10) : topIps);

		return result;
	}

	private OffsetDateTime getStartTime(String timeRange) {
		OffsetDateTime now = OffsetDateTime.now();
		return switch (timeRange) {
			case "1h" -> now.minusHours(1);
			case "24h" -> now.minusHours(24);
			case "7d" -> now.minusDays(7);
			case "30d" -> now.minusDays(30);
			default -> now.minusHours(24);
		};
	}

	public OffsetDateTime parseCustomTimeRange(String start, String end) {
		// 用于自定义时间范围解析
		return OffsetDateTime.parse(start);
	}
}

