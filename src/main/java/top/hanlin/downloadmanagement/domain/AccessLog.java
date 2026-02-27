package top.hanlin.downloadmanagement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "access_log", indexes = {
		@Index(name = "idx_access_ip_time", columnList = "ip,occurredAt"),
		@Index(name = "idx_access_occurred", columnList = "occurredAt")
})
public class AccessLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 45)
	private String ip;

	@Column(nullable = false)
	private String path;

	@Column(nullable = false)
	private OffsetDateTime occurredAt = OffsetDateTime.now();

	@Column
	private Long bytesTransferred; // 下载字节数

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getIp() { return ip; }
	public void setIp(String ip) { this.ip = ip; }
	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }
	public OffsetDateTime getOccurredAt() { return occurredAt; }
	public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
	public Long getBytesTransferred() { return bytesTransferred; }
	public void setBytesTransferred(Long bytesTransferred) { this.bytesTransferred = bytesTransferred; }
}

