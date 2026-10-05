package com.example.cashCombine.api

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class ApiRequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger("api")
    private val messagePattern = Regex("\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI ?: return true
        return !path.startsWith("/api/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val wrapped = ContentCachingResponseWrapper(response)
        val started = System.nanoTime()
        try {
            filterChain.doFilter(request, wrapped)
        } finally {
            val status = wrapped.status
            val elapsedMs = (System.nanoTime() - started) / 1_000_000L
            var line = "${request.method} ${request.requestURI} → $status"
            if (status >= 400) {
                val reason = rejectionReason(wrapped)
                line += if (reason.isNotEmpty()) " rejected: $reason" else " rejected"
                log.warn("$line (${elapsedMs}ms)")
            } else {
                log.info("$line (${elapsedMs}ms)")
            }
            wrapped.copyBodyToResponse()
        }
    }

    private fun rejectionReason(response: ContentCachingResponseWrapper): String {
        val body = response.contentAsByteArray
        if (body.isEmpty()) return ""
        val raw = String(body, Charsets.UTF_8).trim()
        val match = messagePattern.find(raw)
        if (match != null) {
            return match.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
        }
        return if (raw.length > 160) raw.substring(0, 160) + "\u2026" else raw
    }
}