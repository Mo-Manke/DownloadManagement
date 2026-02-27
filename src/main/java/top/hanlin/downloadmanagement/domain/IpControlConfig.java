package top.hanlin.downloadmanagement.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "ip_control_config")
public class IpControlConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String configKey;

	@Column(nullable = false, length = 500)
	private String configValue;

	@Column(length = 500)
	private String description;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getConfigKey() { return configKey; }
	public void setConfigKey(String configKey) { this.configKey = configKey; }
	public String getConfigValue() { return configValue; }
	public void setConfigValue(String configValue) { this.configValue = configValue; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
}

