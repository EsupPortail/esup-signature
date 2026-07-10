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

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class CspReportController {

    private static final Logger logger = LoggerFactory.getLogger("CSP_VIOLATION");
    private static final int MAX_LOGGED_VALUE_LENGTH = 500;
    private static final long DUPLICATE_TTL_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final int MAX_DEDUPLICATION_KEYS = 1000;
    private static final TypeReference<Object> OBJECT_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Map<String, Long> alreadyLoggedReports = new ConcurrentHashMap<>();

    public CspReportController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/csp-report")
    public ResponseEntity<Void> report(@RequestBody(required = false) String body, HttpServletRequest request) {
        Object payload;
        try {
            payload = objectMapper.readValue(body == null ? "{}" : body, OBJECT_TYPE);
        } catch (Exception e) {
            logger.warn("Invalid CSP report remoteAddr={} userAgent={} body={}", request.getRemoteAddr(), sanitize(request.getHeader("User-Agent")), sanitize(body));
            return ResponseEntity.noContent().build();
        }
        Map<String, Object> report = getReport(payload);
        if(isIgnoredReport(report)) {
            return ResponseEntity.noContent().build();
        }
        if(isDuplicate(report)) {
            return ResponseEntity.noContent().build();
        }
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

    private boolean isIgnoredReport(Map<String, Object> report) {
        String effectiveDirective = value(report, "effective-directive");
        String sourceFile = value(report, "source-file");
        String lineNumber = value(report, "line-number");
        return "script-src-elem".equals(effectiveDirective)
                && sourceFile.endsWith("/js/modules/ui/signrequests/Nexu.js")
                && "529".equals(lineNumber);
    }

    private boolean isDuplicate(Map<String, Object> report) {
        long now = System.currentTimeMillis();
        cleanupOldReports(now);
        String key = value(report, "effective-directive") + "|" +
                value(report, "violated-directive") + "|" +
                value(report, "blocked-uri") + "|" +
                value(report, "document-uri") + "|" +
                value(report, "source-file") + "|" +
                value(report, "line-number") + "|" +
                value(report, "column-number") + "|" +
                value(report, "disposition") + "|" +
                value(report, "status-code");
        Long previous = alreadyLoggedReports.putIfAbsent(key, now);
        return previous != null && now - previous < DUPLICATE_TTL_MILLIS;
    }

    private void cleanupOldReports(long now) {
        if(alreadyLoggedReports.size() < MAX_DEDUPLICATION_KEYS) {
            return;
        }
        Iterator<Map.Entry<String, Long>> iterator = alreadyLoggedReports.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if(now - entry.getValue() >= DUPLICATE_TTL_MILLIS) {
                iterator.remove();
            }
        }
        if(alreadyLoggedReports.size() >= MAX_DEDUPLICATION_KEYS) {
            alreadyLoggedReports.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getReport(Object payload) {
        if(payload instanceof List<?> reports && !reports.isEmpty()) {
            Object firstReport = reports.get(0);
            if(firstReport instanceof Map<?, ?> firstReportMap) {
                Object body = firstReportMap.get("body");
                if(body instanceof Map<?, ?>) {
                    return (Map<String, Object>) body;
                }
                return (Map<String, Object>) firstReportMap;
            }
        }
        if(!(payload instanceof Map<?, ?> payloadMap)) {
            return Map.of();
        }
        Object legacyReport = payloadMap.get("csp-report");
        if(legacyReport instanceof Map<?, ?>) {
            return (Map<String, Object>) legacyReport;
        }
        return (Map<String, Object>) payloadMap;
    }

    private String value(Map<String, Object> report, String key) {
        Object value = report.get(key);
        if(value == null) {
            value = report.get(modernReportKey(key));
        }
        return sanitize(value == null ? null : String.valueOf(value));
    }

    private String modernReportKey(String key) {
        return switch (key) {
            case "blocked-uri" -> "blockedURL";
            case "document-uri" -> "documentURL";
            case "effective-directive" -> "effectiveDirective";
            case "violated-directive" -> "effectiveDirective";
            case "source-file" -> "sourceFile";
            case "line-number" -> "lineNumber";
            case "column-number" -> "columnNumber";
            case "status-code" -> "statusCode";
            default -> key;
        };
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
