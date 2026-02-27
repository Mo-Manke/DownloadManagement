package top.hanlin.downloadmanagement.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class DownloadProtectFilter {

	@Bean
	public OncePerRequestFilter rateLimitAndAntiHotlinkFilter(
			@Value("${app.download.rateLimit.perSecond:5}") int perSecond,
			@Value("${app.download.refererWhitelist:}") String refererWhitelist
	) {
		final Map<String, Window> limiters = new ConcurrentHashMap<>();
		final java.util.Set<String> whitelist = new java.util.HashSet<>();
		for (String host : refererWhitelist.split(",")) { if (!host.isBlank()) whitelist.add(host.trim().toLowerCase()); }

		return new OncePerRequestFilter() {
			@Override
			protected boolean shouldNotFilter(HttpServletRequest request) {
				String uri = request.getRequestURI();
				return !(uri.startsWith("/d/") || (uri.equals("/download")));
			}

			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
				// Anti-hotlink by Referer whitelist (optional)
				String ref = request.getHeader("Referer");
				if (!whitelist.isEmpty() && ref != null && !ref.isBlank()) {
					try {
						java.net.URI r = java.net.URI.create(ref);
						String host = (r.getHost() == null ? "" : r.getHost().toLowerCase());
						if (!whitelist.contains(host)) {
							response.setStatus(403);
							response.getWriter().write("Forbidden");
							return;
						}
					} catch (Exception ignored) {}
				}

				// Simple IP rate limit (fixed window per second)
				String key = request.getRemoteAddr();
				Window w = limiters.computeIfAbsent(key, k -> new Window());
				long now = Instant.now().getEpochSecond();
				if (w.second != now) { w.second = now; w.count = 0; }
				if (++w.count > perSecond) {
					response.setStatus(429);
					response.getWriter().write("Too Many Requests");
					return;
				}

				response.setHeader("Accept-Ranges", "bytes");
				filterChain.doFilter(request, response);
			}
		};
	}

	private static class Window { volatile long second; volatile int count; }
}


