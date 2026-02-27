package top.hanlin.downloadmanagement.controller.dto;

public class PackageView {

	public Long id;
	public String name;
	public String version;
	public String flag;
	public boolean enabled;
	public Long ownerId;
	public String ownerUsername;

	public static PackageView of(Long id, String name, String version, String flag, boolean enabled, Long ownerId, String ownerUsername) {
		PackageView v = new PackageView();
		v.id = id;
		v.name = name;
		v.version = version;
		v.flag = flag;
		v.enabled = enabled;
		v.ownerId = ownerId;
		v.ownerUsername = ownerUsername;
		return v;
	}
}


