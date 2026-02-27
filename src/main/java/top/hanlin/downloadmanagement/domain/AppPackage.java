package top.hanlin.downloadmanagement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "app_package", indexes = {
		@Index(name = "idx_app_package_flag", columnList = "flag")
})
public class AppPackage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String version;

	@Column(nullable = false)
	private String flag;

	@Column(nullable = false)
	private boolean enabled = true;

	// 归属管理员
	@Column(nullable = false)
	private Long ownerId;

	@Column(nullable = false)
	private String filePath;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(nullable = false)
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getVersion() { return version; }
	public void setVersion(String version) { this.version = version; }

	public String getFlag() { return flag; }
	public void setFlag(String flag) { this.flag = flag; }

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }

	public Long getOwnerId() { return ownerId; }
	public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

	public String getFilePath() { return filePath; }
	public void setFilePath(String filePath) { this.filePath = filePath; }

	public OffsetDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

	public OffsetDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}


