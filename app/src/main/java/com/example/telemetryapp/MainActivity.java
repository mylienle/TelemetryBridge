package com.example.telemetryapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.xcr.telemetrybridge.SeverityLevel;
import com.xcr.telemetrybridge.TelemetryBridge;
import com.xcr.telemetrybridge.TelemetryConfiguration;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TelemetryConfiguration config = new TelemetryConfiguration();
        config.setCloudRoleName("com.xcrsek.certification.sek");
        config.setCloudRoleInstance("localhost");
        config.setAppVersion("vQA_1.2.29");
        config.setTimeoutSeconds(10);
        config.setSamplingRatio(0.2);

        TelemetryBridge.initialize("https://gj6hx7pd-5001.asse.devtunnels.ms/logs", config);

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
                throw new RuntimeException("Reason why logon failed: 10");
            } catch (Exception e) {
                Map<String, String> exceptionProps = new HashMap<>();
                exceptionProps.put("Message", "IsFunctionAvailable");
                exceptionProps.put("Member", "IsFunctionAvailable");
                exceptionProps.put("Line", "111");
                exceptionProps.put("File", "/Library/DATA/WORK/XCR/PayApp/XAC/CloudBanking/CloudBanking.Shell/CloudBanking.ShellContainer/Functions/Offline.cs");
                exceptionProps.put("SerialNumber", "F21010143801038");
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
            TelemetryBridge.trackEvent("CustomFeatureUsed", eventProps, eventMetrics);
        });
        layout.addView(btnCustomEvent);

        Map<String, String> custom = new HashMap<>();
        custom.put("PackageName", "com.xcrsek.certification.sek");
        custom.put("PlatformOs", "10");
        custom.put("LocalDateTime", "03/04/2025 23:47:26");
        custom.put("AppVersion", "vQA_1.2.29");
        custom.put("Member", "DownloadParams");
        custom.put("Line", "80");
        custom.put("Message", "TMS download failed");
        custom.put("Platform", "Android");
        custom.put("File", "C:\\Users\\Antonio\\source\\vcxpros-android\\CloudBanking\\CloudBanking.Shell\\CloudBanking.Hardware\\CloudBanking.PaxSdk.Services\\TMSService.cs");

        TelemetryBridge.trackTrace("TMS download failed", SeverityLevel.INFO, custom);

        Map<String, String> exceptionProps = new HashMap<>();
        exceptionProps.put("Message", "IsFunctionAvailable");
        exceptionProps.put("Member", "IsFunctionAvailable");
        exceptionProps.put("Line", "111");
        exceptionProps.put("File", "/Library/DATA/WORK/XCR/PayApp/XAC/CloudBanking/CloudBanking.Shell/CloudBanking.ShellContainer/Functions/Offline.cs");
        exceptionProps.put("SerialNumber", "F21010143801038");
        TelemetryBridge.trackException(new Exception("Reason why logon failed: 10"), exceptionProps);
        
        setContentView(layout);
    }
} 