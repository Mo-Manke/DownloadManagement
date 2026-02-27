package top.hanlin.downloadmanagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.hanlin.downloadmanagement.service.DownloadThrottleService;
import top.hanlin.downloadmanagement.service.IpControlService;
import top.hanlin.downloadmanagement.service.IpStatService;
import top.hanlin.downloadmanagement.service.PackageService;
import top.hanlin.downloadmanagement.service.StorageService;
import top.hanlin.downloadmanagement.domain.InstallEvent;
import top.hanlin.downloadmanagement.repository.InstallEventRepository;
import top.hanlin.downloadmanagement.util.IpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Controller
public class DownloadController {

    private final PackageService packageService;
    private final StorageService storageService;
    private final InstallEventRepository installEventRepository;
    private final DownloadThrottleService throttleService;
    private final IpControlService ipControlService;
    private final IpStatService ipStatService;

    public DownloadController(PackageService packageService, StorageService storageService, 
            InstallEventRepository installEventRepository, DownloadThrottleService throttleService,
            IpControlService ipControlService, IpStatService ipStatService) {
        this.packageService = packageService;
        this.storageService = storageService;
        this.installEventRepository = installEventRepository;
        this.throttleService = throttleService;
        this.ipControlService = ipControlService;
        this.ipStatService = ipStatService;
    }

	@GetMapping("/d/{username}/{flag}")
    public ResponseEntity<?> downloadByPath(@PathVariable String username, @PathVariable String flag) {
		return doDownload(username, flag);
	}

    private ResponseEntity<?> doDownload(String username, String flag) {
		var pkgOpt = username != null ? 
			packageService.findEnabledByOwnerAndFlag(username, flag) : 
			packageService.findEnabledByFlag(flag);
		if (pkgOpt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		var pkg = pkgOpt.get();
		FileSystemResource resource = storageService.loadAsResource(pkg.getFilePath());
		if (!resource.exists()) {
			return ResponseEntity.notFound().build();
		}
        // 记录安装事件
        InstallEvent ev = new InstallEvent();
        ev.setPackageId(pkg.getId());
        ev.setOwnerId(pkg.getOwnerId());
        installEventRepository.save(ev);

        // 使用URL路径中的flag作为下载文件名，如 /d/admin/fish 下载为 fish.apk
        String downloadFilename = flag + ".apk";
        // 对文件名进行URL编码，支持中文文件名
        String encodedFilename = URLEncoder.encode(downloadFilename, StandardCharsets.UTF_8).replace("+", "%20");
        // 构建符合RFC 5987的Content-Disposition头
        String contentDisposition = "attachment; filename=\"" + downloadFilename + "\"; filename*=UTF-8''" + encodedFilename;
        long fileLength = resource.getFile().length();

        // 获取客户端IP并记录下载
        String clientIp = getClientIp();
        ipControlService.recordAccess(clientIp, "/d/" + username + "/" + flag, fileLength);
        // 异步记录统计信息
        ipStatService.recordAccess(clientIp, fileLength);

        String range = null;
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) range = attrs.getRequest().getHeader("Range");
        } catch (Exception ignored) {}

        if (range == null || !range.startsWith("bytes=")) {
            // 使用限流包装
            try {
                InputStream throttledStream = throttleService.throttleInputStream(resource.getInputStream(), clientIp);
                InputStreamResource throttledResource = new InputStreamResource(throttledStream) {
                    @Override public long contentLength() throws IOException { return fileLength; }
                };
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                        .contentLength(fileLength)
                        .body(throttledResource);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().build();
            }
        }

        try {
            long start = 0, end = fileLength - 1;
            String spec = range.substring("bytes=".length());
            String[] parts = spec.split("-");
            if (!parts[0].isEmpty()) start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1]);
            if (end >= fileLength) end = fileLength - 1;
            long contentLength = end - start + 1;

            RandomAccessFile raf = new RandomAccessFile(resource.getFile(), "r");
            raf.seek(start);
            InputStream rangeStream = new java.io.FilterInputStream(new java.io.FileInputStream(raf.getFD())) {
                @Override public void close() throws IOException { super.close(); raf.close(); }
            };
            InputStream throttledStream = throttleService.throttleInputStream(rangeStream, clientIp);
            long finalContentLength = contentLength;
            InputStreamResource body = new InputStreamResource(throttledStream) {
                @Override public long contentLength() throws IOException { return finalContentLength; }
            };

            return ResponseEntity.status(206)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                    .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                    .contentLength(contentLength)
                    .body(body);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
	}

	/**
	 * 获取客户端真实IP
	 * 使用IpUtils安全获取，防止伪造X-Forwarded-For等请求头绕过封禁
	 */
	private String getClientIp() {
		try {
			var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attrs != null) {
				return IpUtils.getClientIp(attrs.getRequest());
			}
		} catch (Exception ignored) {}
		return "unknown";
	}

}


