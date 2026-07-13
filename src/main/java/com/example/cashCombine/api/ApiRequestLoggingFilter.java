package com.example.cashCombine.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Concise /api request log: {@code METHOD path → status} and rejection reasons on 4xx/5xx.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger("api");
	private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path == null || !path.startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
		long started = System.nanoTime();
		try {
			filterChain.doFilter(request, wrapped);
		} finally {
			int status = wrapped.getStatus();
			long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
			String line = request.getMethod() + " " + request.getRequestURI() + " → " + status;
			if (status >= 400) {
				String reason = rejectionReason(wrapped);
				if (!reason.isBlank()) {
					line += " rejected: " + reason;
				} else {
					line += " rejected";
				}
				log.warn("{} ({}ms)", line, elapsedMs);
			} else {
				log.info("{} ({}ms)", line, elapsedMs);
			}
			wrapped.copyBodyToResponse();
		}
	}

	private String rejectionReason(ContentCachingResponseWrapper response) {
		byte[] body = response.getContentAsByteArray();
		if (body.length == 0) {
			return "";
		}
		String raw = new String(body, StandardCharsets.UTF_8).trim();
		Matcher matcher = MESSAGE_PATTERN.matcher(raw);
		if (matcher.find()) {
			return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
		}
		return raw.length() > 160 ? raw.substring(0, 160) + "…" : raw;
	}

}
