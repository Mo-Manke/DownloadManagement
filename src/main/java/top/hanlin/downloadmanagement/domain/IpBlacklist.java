package top.hanlin.downloadmanagement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ip_blacklist", indexes = {@Index(name = "uk_ip_blacklist_ip", columnList = "ip", unique = true)})
public class IpBlacklist {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 45)
	private String ip;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column
	private OffsetDateTime expiresAt; // null表示永久

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getIp() { return ip; }
	public void setIp(String ip) { this.ip = ip; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public OffsetDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
	public OffsetDateTime getExpiresAt() { return expiresAt; }
	public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}

