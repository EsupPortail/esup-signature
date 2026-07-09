package org.esupportail.esupsignature.web.log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CspReportController {

    private static final Logger logger = LoggerFactory.getLogger("CSP_VIOLATION");
    private static final int MAX_LOGGED_VALUE_LENGTH = 500;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public CspReportController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/csp-report")
    public ResponseEntity<Void> report(@RequestBody(required = false) String body, HttpServletRequest request) {
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(body == null ? "{}" : body, MAP_TYPE);
        } catch (Exception e) {
            logger.warn("Invalid CSP report remoteAddr={} userAgent={} body={}", request.getRemoteAddr(), sanitize(request.getHeader("User-Agent")), sanitize(body));
            return ResponseEntity.noContent().build();
        }
        Map<String, Object> report = getReport(payload);
        logger.info("effectiveDirective={} violatedDirective={} blockedUri={} documentUri={} sourceFile={} lineNumber={} columnNumber={} disposition={} statusCode={} remoteAddr={} userAgent={}",
                value(report, "effective-directive"),
                value(report, "violated-directive"),
                value(report, "blocked-uri"),
                value(report, "document-uri"),
                value(report, "source-file"),
                value(report, "line-number"),
                value(report, "column-number"),
                value(report, "disposition"),
                value(report, "status-code"),
                request.getRemoteAddr(),
                sanitize(request.getHeader("User-Agent")));
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getReport(Map<String, Object> payload) {
        Object legacyReport = payload.get("csp-report");
        if(legacyReport instanceof Map<?, ?>) {
            return (Map<String, Object>) legacyReport;
        }
        return payload;
    }

    private String value(Map<String, Object> report, String key) {
        Object value = report.get(key);
        return sanitize(value == null ? null : String.valueOf(value));
    }

    private String sanitize(String value) {
        if(value == null) {
            return "";
        }
        String sanitized = value.replace('\n', ' ').replace('\r', ' ');
        if(sanitized.length() > MAX_LOGGED_VALUE_LENGTH) {
            return sanitized.substring(0, MAX_LOGGED_VALUE_LENGTH);
        }
        return sanitized;
    }

}
