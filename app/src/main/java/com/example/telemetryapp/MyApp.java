package com.example.telemetryapp;

import android.app.Application;
import com.example.telemetrybridge.TelemetryBridge;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TelemetryBridge.initialize("https://gj6hx7pd-5001.asse.devtunnels.ms/logs");
        TelemetryBridge.setUserId("user123");
        TelemetryBridge.setSessionId("session456");
    }
} 