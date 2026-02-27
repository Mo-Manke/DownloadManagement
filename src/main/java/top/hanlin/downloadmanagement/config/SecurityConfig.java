package top.hanlin.downloadmanagement.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import top.hanlin.downloadmanagement.config.IpControlFilter;
import top.hanlin.downloadmanagement.domain.AdminUser;
import top.hanlin.downloadmanagement.repository.AdminUserRepository;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, PersistentTokenRepository tokenRepository, UserDetailsService userDetailsService, IpControlFilter ipControlFilter) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/login", "/captcha", "/auth/forgot", "/d/**", "/download", "/css/**", "/js/**").permitAll()
				.requestMatchers("/admin/users/**", "/admin/requests/**").hasRole("SUPER_ADMIN")
				.anyRequest().authenticated()
			)
			.formLogin(form -> form
				.loginPage("/login")
				.defaultSuccessUrl("/admin", true)
				.permitAll()
			)
			.userDetailsService(userDetailsService)
			.rememberMe(r -> r
				.tokenRepository(tokenRepository)
				.tokenValiditySeconds(3 * 24 * 60 * 60)
				.rememberMeParameter("remember-me")
			)
			.logout(Customizer.withDefaults())
			.addFilterBefore(ipControlFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(captchaFilter(), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PersistentTokenRepository persistentTokenRepository(DataSource dataSource, JdbcTemplate jdbcTemplate) {
		JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
		repo.setDataSource(dataSource);
		// 确保表存在
		try {
			jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS persistent_logins (\n" +
					"username VARCHAR(64) NOT NULL,\n" +
					"series VARCHAR(64) PRIMARY KEY,\n" +
					"token VARCHAR(64) NOT NULL,\n" +
					"last_used TIMESTAMP NOT NULL\n" +
					")");
		} catch (Exception e) {
			// 表可能已存在，忽略
		}
		return repo;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public OncePerRequestFilter captchaFilter() {
		return new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
				if ("/login".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
					String input = request.getParameter("captcha");
					Object expect = request.getSession().getAttribute("captcha");
					if (expect == null || !StringUtils.hasText(input) || !expect.toString().equalsIgnoreCase(input.trim())) {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.setContentType("text/plain;charset=UTF-8");
						response.getWriter().write("验证码错误");
						return;
					}
				}
				filterChain.doFilter(request, response);
			}
		};
	}

	@Service
	public static class DbUserDetailsService implements UserDetailsService {
		private final AdminUserRepository repository;

		public DbUserDetailsService(AdminUserRepository repository) { this.repository = repository; }

		@Override
		public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
			AdminUser user = repository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("not found"));
			return org.springframework.security.core.userdetails.User
					.withUsername(user.getUsername())
					.password(user.getPassword())
					.disabled(!user.isEnabled())
					.roles(user.getRole() == AdminUser.Role.SUPER_ADMIN ? "SUPER_ADMIN" : "ADMIN")
					.build();
		}
	}
}


