package top.hanlin.downloadmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;

@Controller
public class CaptchaController {

	private static final SecureRandom RANDOM = new SecureRandom();

	@GetMapping("/captcha")
	public void captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
		int a = 1 + RANDOM.nextInt(9);
		int b = 1 + RANDOM.nextInt(9);
		int sum = a + b;
		request.getSession().setAttribute("captcha", String.valueOf(sum));
		response.setContentType("image/svg+xml;charset=UTF-8");
		String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='120' height='40'>" +
				"<rect width='100%' height='100%' fill='#f3f4f6'/>" +
				"<text x='50%' y='50%' dominant-baseline='middle' text-anchor='middle' font-size='20' fill='#111827'>" + a + "+" + b + "=?" + "</text>" +
				"</svg>";
		response.getWriter().write(svg);
	}
}


