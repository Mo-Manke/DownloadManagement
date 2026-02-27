package top.hanlin.downloadmanagement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "account_request", indexes = {@Index(name = "idx_req_status", columnList = "status")})
public class AccountRequest {

	public enum Type { USERNAME_CHANGE, PASSWORD_RESET }
	public enum Status { PENDING, APPROVED, REJECTED }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Type type;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Status status = Status.PENDING;

	@Column(nullable = false)
	private Long requesterId; // 发起人（普通管理员或自己）

	@Column(nullable = false)
	private Long targetUserId; // 被修改的账号（可以等于 requesterId）

	@Column
	private String newUsername;

	@Column
	private String newPasswordHash;

	@Column(nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Type getType() { return type; }
	public void setType(Type type) { this.type = type; }
	public Status getStatus() { return status; }
	public void setStatus(Status status) { this.status = status; }
	public Long getRequesterId() { return requesterId; }
	public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }
	public Long getTargetUserId() { return targetUserId; }
	public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
	public String getNewUsername() { return newUsername; }
	public void setNewUsername(String newUsername) { this.newUsername = newUsername; }
	public String getNewPasswordHash() { return newPasswordHash; }
	public void setNewPasswordHash(String newPasswordHash) { this.newPasswordHash = newPasswordHash; }
	public OffsetDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}


