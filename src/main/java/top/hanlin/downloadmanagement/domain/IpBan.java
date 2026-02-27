package top.hanlin.downloadmanagement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ip_ban", indexes = {
		@Index(name = "idx_ip_ban_ip", columnList = "ip"),
		@Index(name = "idx_ip_ban_active", columnList = "active,expiresAt")
})
public class IpBan {

	public enum BanType { MANUAL, AUTO }
	public enum BanReason { MANUAL_BAN, RATE_LIMIT_EXCEEDED, CONCURRENT_EXCEEDED, DOWNLOAD_RATE_EXCEEDED }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 45)
	private String ip;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BanType banType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private BanReason reason;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column
	private OffsetDateTime expiresAt; // null表示永久封禁

	@Column(nullable = false)
	private boolean active = true;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getIp() { return ip; }
	public void setIp(String ip) { this.ip = ip; }
	public BanType getBanType() { return banType; }
	public void setBanType(BanType banType) { this.banType = banType; }
	public BanReason getReason() { return reason; }
	public void setReason(BanReason reason) { this.reason = reason; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public OffsetDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
	public OffsetDateTime getExpiresAt() { return expiresAt; }
	public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
}

