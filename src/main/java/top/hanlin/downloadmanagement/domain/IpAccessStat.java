package top.hanlin.downloadmanagement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ip_access_stat", indexes = {
		@Index(name = "idx_ip_stat_ip_time", columnList = "ip,occurredAt"),
		@Index(name = "idx_ip_stat_time", columnList = "occurredAt"),
		@Index(name = "idx_ip_stat_server", columnList = "serverId")
})
public class IpAccessStat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 45)
	private String ip;

	@Column(nullable = false)
	private OffsetDateTime occurredAt;

	@Column(nullable = false)
	private Long requestCount = 1L;

	@Column(nullable = false)
	private Long downloadBytes = 0L;

	@Column(nullable = false, length = 50)
	private String serverId;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getIp() { return ip; }
	public void setIp(String ip) { this.ip = ip; }
	public OffsetDateTime getOccurredAt() { return occurredAt; }
	public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
	public Long getRequestCount() { return requestCount; }
	public void setRequestCount(Long requestCount) { this.requestCount = requestCount; }
	public Long getDownloadBytes() { return downloadBytes; }
	public void setDownloadBytes(Long downloadBytes) { this.downloadBytes = downloadBytes; }
	public String getServerId() { return serverId; }
	public void setServerId(String serverId) { this.serverId = serverId; }
}

