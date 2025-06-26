package com.example.telemetrybridge;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import io.opentelemetry.api.GlobalOpenTelemetry;

public class TelemetryBridge {
    private static Tracer tracer;
    private static OpenTelemetry openTelemetry;
    private static Meter meter;
    private static String userId;
    private static String sessionId;
    private static String deviceId;
    private static String appVersion;
    private static Map<String, String> customDimensions = new HashMap<>();

    private static final Set<String> SENSITIVE_KEYS = new java.util.HashSet<>(Arrays.asList(
            "password", "pass", "pwd", "credit_card", "cc", "ssn", "secret", "token", "auth", "authorization"
    ));
    private static final String REDACTED = "[REDACTED]";

    public static void initialize(String endpoint) {
        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .build();

        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        tracer = openTelemetry.getTracer("TelemetryBridge");
        meter = GlobalOpenTelemetry.get().getMeter("TelemetryBridge");
    }

    // Track a simple event (span) with optional properties and metrics
    public static void trackEvent(String name) {
        trackEvent(name, null, null);
    }
    public static void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder(name).startSpan();
        if (properties != null) {
            Map<String, String> filtered = filterSensitive(properties);
            for (Map.Entry<String, String> entry : filtered.entrySet()) {
                span.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        if (metrics != null) {
            for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                span.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        addContext(span);
        span.end();
    }

    // Track a trace/log message with severity
    public static void trackTrace(String message, String severity) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder("Trace").startSpan();
        span.setAttribute("log.severity", severity);
        span.setAttribute("log.message", message);
        addContext(span);
        span.end();
    }

    // Track an exception with optional properties
    public static void trackException(String message) {
        trackException(message, null);
    }
    public static void trackException(String message, Map<String, String> properties) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder("Exception").startSpan();
        span.recordException(new Exception(message));
        if (properties != null) {
            Map<String, String> filtered = filterSensitive(properties);
            for (Map.Entry<String, String> entry : filtered.entrySet()) {
                span.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        addContext(span);
        span.setStatus(StatusCode.ERROR);
        span.end();
    }

    // Track a custom metric
    public static void trackMetric(String name, double value) {
        if (meter == null) return;
        LongCounter counter = meter.counterBuilder(name).build();
        counter.add((long) value);
    }

    // Track a dependency (external call)
    public static void trackDependency(String name, String type, String target, boolean success, long durationMs) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder(name)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        span.setAttribute("dependency.type", type);
        span.setAttribute("dependency.target", target);
        span.setAttribute("dependency.success", success);
        span.setAttribute("dependency.duration_ms", durationMs);
        addContext(span);
        if (!success) span.setStatus(StatusCode.ERROR);
        span.end();
    }

    // Set user context
    public static void setUserId(String id) {
        userId = id;
    }
    public static void setSessionId(String id) {
        sessionId = id;
    }

    // Helper methods for log levels (like Application Insights)
    public static void trackInfo(String message) {
        trackTrace(message, "INFO");
    }

    public static void trackWarning(String message) {
        trackTrace(message, "WARN");
    }

    public static void trackError(String message) {
        trackTrace(message, "ERROR");
    }

    public static void trackDebug(String message) {
        trackTrace(message, "DEBUG");
    }

    // Track HTTP request (like Application Insights TrackRequest)
    public static void trackRequest(String name, String url, String method, long durationMs, boolean success) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder(name)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        span.setAttribute("http.url", url);
        span.setAttribute("http.method", method);
        span.setAttribute("http.duration_ms", durationMs);
        span.setAttribute("http.success", success);
        span.setAttribute("telemetry.type", "request");
        addContext(span);
        if (!success) span.setStatus(StatusCode.ERROR);
        span.end();
    }

    // Track page view/screen view (like Application Insights TrackPageView)
    public static void trackPageView(String pageName) {
        trackPageView(pageName, null, null);
    }

    public static void trackPageView(String pageName, Map<String, String> properties, Map<String, Double> metrics) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder("PageView")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        span.setAttribute("page.name", pageName);
        span.setAttribute("telemetry.type", "pageview");
        if (properties != null) {
            Map<String, String> filtered = filterSensitive(properties);
            for (Map.Entry<String, String> entry : filtered.entrySet()) {
                span.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        if (metrics != null) {
            for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                span.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        addContext(span);
        span.end();
    }

    // Track availability/health check (like Application Insights TrackAvailability)
    public static void trackAvailability(String name, long durationMs, boolean success, String message) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder("Availability")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        span.setAttribute("availability.name", name);
        span.setAttribute("availability.duration_ms", durationMs);
        span.setAttribute("availability.success", success);
        span.setAttribute("availability.message", message);
        span.setAttribute("telemetry.type", "availability");
        addContext(span);
        if (!success) span.setStatus(StatusCode.ERROR);
        span.end();
    }

    // Enhanced dependency tracking with more details
    public static void trackDependency(String name, String type, String target, String data,boolean success, long durationMs) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder(name)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        span.setAttribute("dependency.type", type);
        span.setAttribute("dependency.target", target);
        span.setAttribute("dependency.success", success);
        span.setAttribute("dependency.duration_ms", durationMs);
        span.setAttribute("telemetry.type", "dependency");
        addContext(span);
        if (!success) span.setStatus(StatusCode.ERROR);
        span.end();
    }

    // Track custom event with timestamp
    public static void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics, long timestamp) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder(name).startSpan();
        span.setAttribute("event.timestamp", timestamp);
        if (properties != null) {
            Map<String, String> filtered = filterSensitive(properties);
            for (Map.Entry<String, String> entry : filtered.entrySet()) {
                span.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        if (metrics != null) {
            for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                span.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        addContext(span);
        span.end();
    }

    // Set device context (like Application Insights device context)
    public static void setDeviceId(String id) {
        deviceId = id;
    }

    public static void setAppVersion(String version) {
        appVersion = version;
    }

    public static void setCustomDimension(String key, String value) {
        customDimensions.put(key, value);
    }

    public static void clearCustomDimensions() {
        customDimensions.clear();
    }

    // Enhanced context with device info and custom dimensions
    private static void addContext(Span span) {
        if (userId != null) span.setAttribute("user.id", userId);
        if (sessionId != null) span.setAttribute("session.id", sessionId);
        if (deviceId != null) span.setAttribute("device.id", deviceId);
        if (appVersion != null) span.setAttribute("app.version", appVersion);

        // Add custom dimensions
        for (Map.Entry<String, String> entry : customDimensions.entrySet()) {
            span.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    // Track metric with custom properties
    public static void trackMetric(String name, double value, Map<String, String> properties) {
        if (meter == null) return;
        LongCounter counter = meter.counterBuilder(name).build();
        counter.add((long) value);

        // Also track as span for consistency
        if (tracer != null) {
            Span span = tracer.spanBuilder("Metric").startSpan();
            span.setAttribute("metric.name", name);
            span.setAttribute("metric.value", value);
            span.setAttribute("telemetry.type", "metric");
            if (properties != null) {
                Map<String, String> filtered = filterSensitive(properties);
                for (Map.Entry<String, String> entry : filtered.entrySet()) {
                    span.setAttribute(entry.getKey(), entry.getValue());
                }
            }
            addContext(span);
            span.end();
        }
    }

    // Track exception with stack trace
    public static void trackException(Exception exception, Map<String, String> properties) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder("Exception").startSpan();
        span.recordException(exception);
        span.setAttribute("exception.type", exception.getClass().getSimpleName());
        span.setAttribute("telemetry.type", "exception");
        if (properties != null) {
            Map<String, String> filtered = filterSensitive(properties);
            for (Map.Entry<String, String> entry : filtered.entrySet()) {
                span.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        addContext(span);
        span.setStatus(StatusCode.ERROR);
        span.end();
    }

    private static Map<String, String> filterSensitive(Map<String, String> input) {
        Map<String, String> filtered = new HashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String key = entry.getKey().toLowerCase();
            boolean isSensitive = SENSITIVE_KEYS.stream().anyMatch(key::contains);
            filtered.put(entry.getKey(), isSensitive ? REDACTED : entry.getValue());
        }
        return filtered;
    }
}