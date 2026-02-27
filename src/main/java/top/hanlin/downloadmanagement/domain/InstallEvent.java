package top.hanlin.downloadmanagement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "install_event", indexes = {
		@Index(name = "idx_install_pkg_time", columnList = "packageId,occurredAt")
})
public class InstallEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long packageId;

	@Column(nullable = false)
	private Long ownerId;

	@Column(nullable = false)
	private OffsetDateTime occurredAt = OffsetDateTime.now();

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getPackageId() { return packageId; }
	public void setPackageId(Long packageId) { this.packageId = packageId; }
	public Long getOwnerId() { return ownerId; }
	public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
	public OffsetDateTime getOccurredAt() { return occurredAt; }
	public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
}


