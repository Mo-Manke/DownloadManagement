package top.hanlin.downloadmanagement.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.hanlin.downloadmanagement.service.IpControlService;
import top.hanlin.downloadmanagement.util.IpUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class IpControlFilter extends OncePerRequestFilter {

	@Autowired
	private IpControlService ipControlService;

	/**
	 * 可信代理IP列表，多个IP用逗号分隔
	 * 只有来自这些IP的请求才会信任X-Forwarded-For等头
	 * 如果为空，则不信任任何转发头，直接使用remoteAddr
	 */
	@Value("${app.ip.trustedProxies:}")
	private String trustedProxiesConfig;

	@PostConstruct
	public void init() {
		// 初始化可信代理列表
		if (trustedProxiesConfig != null && !trustedProxiesConfig.trim().isEmpty()) {
			Set<String> proxies = new HashSet<>(Arrays.asList(trustedProxiesConfig.split(",")));
			proxies.removeIf(String::isEmpty);
			IpUtils.setTrustedProxies(proxies);
		}
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String path = request.getRequestURI();

		// 检查路径是否需要应用IP控制
		if (!ipControlService.shouldApplyIpControl(path)) {
			filterChain.doFilter(request, response);
			return;
		}

		String ip = getClientIp(request);

		// 检查黑名单
		if (ipControlService.isBlacklisted(ip)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType("text/plain;charset=UTF-8");
			response.getWriter().write("IP已被加入黑名单");
			return;
		}

		// 检查封禁
		if (ipControlService.isBanned(ip)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType("text/plain;charset=UTF-8");
			response.getWriter().write("IP已被封禁");
			return;
		}

		// 检查访问频率限制
		if (!ipControlService.checkRateLimit(ip)) {
			response.setStatus(429); // 429 Too Many Requests
			response.setContentType("text/plain;charset=UTF-8");
			response.getWriter().write("访问过于频繁，请稍后再试");
			return;
		}

		// 检查并发限制
		if (!ipControlService.checkConcurrentLimit(ip)) {
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			response.setContentType("text/plain;charset=UTF-8");
			response.getWriter().write("并发连接数过多");
			return;
		}

		// 记录访问（异步，避免阻塞）
		try {
			ipControlService.incrementConcurrent(ip);
			ipControlService.recordAccess(ip, path, null);
		} catch (Exception e) {
			// 记录失败不影响请求
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			ipControlService.decrementConcurrent(ip);
		}
	}

	/**
	 * 获取客户端真实IP
	 * 使用IpUtils安全获取，防止伪造X-Forwarded-For等请求头绕过封禁
	 */
	private String getClientIp(HttpServletRequest request) {
		return IpUtils.getClientIp(request);
	}
}

