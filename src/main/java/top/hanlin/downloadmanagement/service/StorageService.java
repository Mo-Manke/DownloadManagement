package top.hanlin.downloadmanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

	private final Path storageRoot;

	public StorageService(@Value("${app.storage.location:uploads}") String storageLocation) throws IOException {
		this.storageRoot = Path.of(storageLocation).toAbsolutePath().normalize();
		Files.createDirectories(this.storageRoot);
	}

	public String storeApk(MultipartFile file) throws IOException {
		String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file.apk" : file.getOriginalFilename());
		String ext = original.toLowerCase().endsWith(".apk") ? ".apk" : "";
		String filename = UUID.randomUUID() + ext;
		Path target = storageRoot.resolve(filename);
		Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
		return target.toString();
	}

	public FileSystemResource loadAsResource(String absolutePath) {
		return new FileSystemResource(absolutePath);
	}
}


