package top.hanlin.downloadmanagement.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "ip_control_path", indexes = {@Index(name = "uk_ip_control_path", columnList = "path", unique = true)})
public class IpControlPath {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 500)
	private String path;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(length = 500)
	private String description;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
}

