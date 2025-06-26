package com.example.telemetrybridge;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import io.opentelemetry.api.GlobalOpenTelemetry;
import com.google.gson.Gson;

/**
 * TelemetryBridge - A library for recording telemetry (event, trace, exception, metric, dependency)
 * 
 * Usage:
 * 1. Call initialize() with endpoint and TelemetryConfiguration.
 * 2. Use track* methods to record custom telemetry.
 */
public class TelemetryBridge {
    private static Tracer tracer;
    private static OpenTelemetry openTelemetry;
    private static Meter meter;
    private static TelemetryConfiguration configuration;

    /**
     * Initialize TelemetryBridge with endpoint and configuration.
     * @param endpoint OTLP or REST API endpoint to send telemetry.
     * @param config TelemetryConfiguration containing configuration details.
     */
    public static void initialize(String endpoint, TelemetryConfiguration config) {
        configuration = config;
        int timeout = config.getTimeoutSeconds();
        double samplingRatio = config.getSamplingRatio();
        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(timeout, TimeUnit.SECONDS)
                .build();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setSampler(Sampler.traceIdRatioBased(samplingRatio))
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .build();
        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        tracer = openTelemetry.getTracer("TelemetryBridge");
        meter = GlobalOpenTelemetry.get().getMeter("TelemetryBridge");
    }

    /**
     * Flush all remaining logs (similar to Application Insights Flush).
     */
    public static void flush() {
        if (openTelemetry != null && openTelemetry instanceof OpenTelemetrySdk) {
            OpenTelemetrySdk sdk = (OpenTelemetrySdk) openTelemetry;
            sdk.getSdkTracerProvider().forceFlush();
        }
    }

    // --- Track Event ---
    /**
     * Record a simple event.
     * @param name Event name
     */
    public static void trackEvent(String name) {
        trackEvent(name, null, null);
    }
    public static void trackEvent(String name, Map<String, String> properties) {
        trackEvent(name, properties, null);
    }
    /**
     * Record an event with properties and metrics.
     * @param name Event name
     * @param properties Custom properties
     * @param metrics Custom metrics
     */
    public static void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder(name).startSpan();
        span.setAttribute("type", "event");
        span.setAttribute("name", name);
        if (properties != null && !properties.isEmpty()) {
            Map<String, String> filtered = redactSensitive(properties);
            String json = new Gson().toJson(filtered);
            span.setAttribute("properties", json);
        }
        if (metrics != null && !metrics.isEmpty()) {
            String json = new Gson().toJson(metrics);
            span.setAttribute("metrics", json);
        }
        addContext(span);
        span.end();
    }

    // --- Track Trace ---
    /**
     * Record a trace log with message, severity, and properties.
     */
    public static void trackTrace(String message) {
        trackTrace(message, SeverityLevel.INFO, null);
    }
    public static void trackTrace(String message, SeverityLevel severity) {
        trackTrace(message, severity, null);
    }
    public static void trackTrace(String message, Map<String, String> properties) {
        trackTrace(message, SeverityLevel.INFO, properties);
    }
    public static void trackTrace(String message, SeverityLevel severity, Map<String, String> properties) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder("Trace").startSpan();
        span.setAttribute("type", "trace");
        span.setAttribute("message", message);
        span.setAttribute("severityLevel", severity.getValue());
        if (properties != null && !properties.isEmpty()) {
            Map<String, String> filtered = redactSensitive(properties);
            String json = new Gson().toJson(filtered);
            span.setAttribute("properties", json);
        }
        addContext(span);
        span.end();
    }

    // --- Track Exception ---
    /**
     * Record an exception (should be called manually or integrated with UncaughtExceptionHandler).
     */
    public static void trackException(Exception exception) {
        trackException(exception, null, null);
    }
    public static void trackException(Exception exception, Map<String, String> properties) {
        trackException(exception, properties, null);
    }
    public static void trackException(Exception exception, Map<String, String> properties, Map<String, Double> metrics) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder("Exception").startSpan();
        span.recordException(exception);
        span.setAttribute("type", "exception");
        span.setAttribute("exceptionType", exception.getClass().getSimpleName());
        if (properties != null && !properties.isEmpty()) {
            Map<String, String> filtered = redactSensitive(properties);
            String json = new Gson().toJson(filtered);
            span.setAttribute("properties", json);
        }
        if (metrics != null && !metrics.isEmpty()) {
            String json = new Gson().toJson(metrics);
            span.setAttribute("metrics", json);
        }
        addContext(span);
        span.setStatus(StatusCode.ERROR);
        span.end();
    }

    // --- Track Metric ---
    /**
     * Record a custom metric.
     */
    public static void trackMetric(String name, double value) {
        trackMetric(name, value, null);
    }
    public static void trackMetric(String name, double value, Map<String, String> properties) {
        if (meter == null) return;
        LongCounter counter = meter.counterBuilder(name).build();
        counter.add((long) value);
        if (tracer != null) {
            Span span = tracer.spanBuilder("Metric").startSpan();
            span.setAttribute("type", "metric");
            span.setAttribute("name", name);
            span.setAttribute("value", value);
            if (properties != null && !properties.isEmpty()) {
                Map<String, String> filtered = redactSensitive(properties);
                String json = new Gson().toJson(filtered);
                span.setAttribute("properties", json);
            }
            addContext(span);
            span.end();
        }
    }

    // --- Track Dependency ---
    /**
     * Record a dependency (e.g., HTTP, DB call).
     */
    public static void trackDependency(String name, String type, String target, boolean success, long durationMs) {
        trackDependency(name, type, target, success, durationMs, null);
    }
    public static void trackDependency(String name, String type, String target, boolean success, long durationMs, Map<String, String> properties) {
        if (tracer == null) return;
        Span span = tracer.spanBuilder(name)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        span.setAttribute("type", type);
        span.setAttribute("name", name);
        span.setAttribute("target", target);
        span.setAttribute("success", success);
        span.setAttribute("duration", durationMs);
        if (properties != null && !properties.isEmpty()) {
            Map<String, String> filtered = redactSensitive(properties);
            String json = new Gson().toJson(filtered);
            span.setAttribute("properties", json);
        }
        addContext(span);
        if (!success) span.setStatus(StatusCode.ERROR);
        span.end();
    }

    /**
     * Add context (app version, role, key, ...) to the span.
     */
    private static void addContext(Span span) {
        if (configuration != null) {
            if (configuration.getAppVersion() != null)
                span.setAttribute("application_Version", configuration.getAppVersion());
            if (configuration.getCloudRoleName() != null)
                span.setAttribute("cloud_RoleName", configuration.getCloudRoleName());
            if (configuration.getCloudRoleInstance() != null)
                span.setAttribute("cloud_RoleInstance", configuration.getCloudRoleInstance());
            if (configuration.getInstrumentationKey() != null)
                span.setAttribute("instrumentationKey", configuration.getInstrumentationKey());
            if (configuration.getConnectionString() != null)
                span.setAttribute("connectionString", configuration.getConnectionString());
        }
    }

    /**
     * Redact sensitive data according to the configured key list.
     */
    private static Map<String, String> redactSensitive(Map<String, String> input) {
        if (configuration == null || configuration.getSensitiveKeys() == null || configuration.getSensitiveKeys().isEmpty()) return input;
        Map<String, String> filtered = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String key = entry.getKey().toLowerCase();
            boolean isSensitive = false;
            for (String s : configuration.getSensitiveKeys()) {
                if (key.contains(s.toLowerCase())) {
                    isSensitive = true;
                    break;
                }
            }
            filtered.put(entry.getKey(), isSensitive ? "[REDACTED]" : entry.getValue());
        }
        return filtered;
    }
}