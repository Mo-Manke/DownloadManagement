package top.hanlin.downloadmanagement.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.hanlin.downloadmanagement.service.IpControlService;

@Component
public class IpControlCleanupTask {

	private final IpControlService ipControlService;

	public IpControlCleanupTask(IpControlService ipControlService) {
		this.ipControlService = ipControlService;
	}

	@Scheduled(fixedRate = 60000) // 每分钟执行一次
	public void cleanupExpiredBans() {
		ipControlService.cleanupExpiredBans();
	}
}

