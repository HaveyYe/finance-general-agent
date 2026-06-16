package com.zcy.finance.mcp.config;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class McpAuthInterceptor implements HandlerInterceptor {

    private final FinanceMcpProperties properties;

    public McpAuthInterceptor(FinanceMcpProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        FinanceMcpProperties.Auth auth = properties.getAuth();
        if (auth == null || !auth.isEnabled()) {
            return true;
        }

        if (!StringUtils.hasText(auth.getToken())) {
            writeUnauthorized(response, "MCP auth token is not configured");
            return false;
        }

        String actualToken = extractToken(request, auth.getHeaderName());
        if (secureEquals(auth.getToken(), actualToken)) {
            return true;
        }

        writeUnauthorized(response, "Invalid MCP token");
        return false;
    }

    private String extractToken(HttpServletRequest request, String headerName) {
        String configuredHeader = StringUtils.hasText(headerName) ? headerName : "Authorization";
        String value = request.getHeader(configuredHeader);
        if (!StringUtils.hasText(value) && !"Authorization".equalsIgnoreCase(configuredHeader)) {
            value = request.getHeader("Authorization");
        }
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }

    private boolean secureEquals(String expected, String actual) {
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32001,\"message\":\"Unauthorized\",\"data\":\"" + escapeJson(message) + "\"}}");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
