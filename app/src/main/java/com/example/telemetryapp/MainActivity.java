package com.example.telemetryapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.example.telemetrybridge.TelemetryBridge;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up context (like Application Insights)
        TelemetryBridge.setDeviceId("android_device_123");
        TelemetryBridge.setAppVersion("1.0.0");
        TelemetryBridge.setCustomDimension("environment", "development");
        TelemetryBridge.setCustomDimension("app_type", "demo");

        // Track page view
        TelemetryBridge.trackPageView("MainActivity");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // Button 1: Basic Event with Sensitive Data (will be redacted)
        Button btnEvent = new Button(this);
        btnEvent.setText("Track Event (with sensitive data)");
        btnEvent.setOnClickListener(v -> {
            Map<String, String> props = new HashMap<>();
            props.put("screen", "main");
            props.put("username", "alice");
            props.put("password", "supersecret123"); // Will be redacted!
            props.put("credit_card", "1234-5678-9012-3456"); // Will be redacted!
            TelemetryBridge.trackEvent("ButtonClicked", props, null);
        });
        layout.addView(btnEvent);

        // Button 4: Dependencies (like Application Insights TrackDependency)
        Button btnDependency = new Button(this);
        btnDependency.setText("Track Dependencies");
        btnDependency.setOnClickListener(v -> {
            TelemetryBridge.trackDependency("SQL Query", "SQL", "users_db", true, 25);
            TelemetryBridge.trackDependency("HTTP Call", "HTTP", "api.example.com", true, 120);
            TelemetryBridge.trackDependency("File Read", "File", "local_storage", false, 500);
        });
        layout.addView(btnDependency);
        
        // Button 5: Metrics (like Application Insights TrackMetric)
        Button btnMetric = new Button(this);
        btnMetric.setText("Track Metrics");
        btnMetric.setOnClickListener(v -> {
            Map<String, String> metricProps = new HashMap<>();
            metricProps.put("category", "performance");
            metricProps.put("component", "ui");
            TelemetryBridge.trackMetric("PageLoadTime", 2.5, metricProps);
            TelemetryBridge.trackMetric("MemoryUsage", 85.7, null);
            TelemetryBridge.trackMetric("UserActions", 1.0, null);
        });
        layout.addView(btnMetric);

        // Button 7: Exception with Stack Trace
        Button btnException = new Button(this);
        btnException.setText("Track Exception with Stack Trace");
        btnException.setOnClickListener(v -> {
            try {
                throw new RuntimeException("This is a test exception with stack trace");
            } catch (Exception e) {
                Map<String, String> exceptionProps = new HashMap<>();
                exceptionProps.put("screen", "main");
                exceptionProps.put("action", "test_exception");
                TelemetryBridge.trackException(e, exceptionProps);
            }
        });
        layout.addView(btnException);

        // Button 9: Custom Event with Timestamp
        Button btnCustomEvent = new Button(this);
        btnCustomEvent.setText("Track Custom Event with Timestamp");
        btnCustomEvent.setOnClickListener(v -> {
            Map<String, String> eventProps = new HashMap<>();
            eventProps.put("feature", "advanced_telemetry");
            eventProps.put("user_level", "expert");
            
            Map<String, Double> eventMetrics = new HashMap<>();
            eventMetrics.put("feature_usage_count", 5.0);
            
            long timestamp = System.currentTimeMillis();
            TelemetryBridge.trackEvent("CustomFeatureUsed", eventProps, eventMetrics, timestamp);
        });
        layout.addView(btnCustomEvent);
        
        setContentView(layout);
    }
} 