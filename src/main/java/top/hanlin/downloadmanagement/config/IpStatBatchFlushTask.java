package top.hanlin.downloadmanagement.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.hanlin.downloadmanagement.service.IpStatService;

@Component
public class IpStatBatchFlushTask {

	private final IpStatService ipStatService;

	public IpStatBatchFlushTask(IpStatService ipStatService) {
		this.ipStatService = ipStatService;
	}

	@Scheduled(fixedRate = 10000) // 每10秒刷新一次
	public void flushBatch() {
		ipStatService.flushBatch();
	}
}

