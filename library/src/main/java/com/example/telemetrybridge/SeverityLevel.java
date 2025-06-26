package com.example.telemetrybridge;

public enum SeverityLevel {
    VERBOSE(0), INFO(1), WARN(2), ERROR(3), CRITICAL(4);
    private final int value;
    SeverityLevel(int value) { this.value = value; }
    public int getValue() { return value; }
    public static SeverityLevel fromString(String s) {
        switch (s.toUpperCase()) {
            case "VERBOSE": return VERBOSE;
            case "INFO": return INFO;
            case "WARN": return WARN;
            case "ERROR": return ERROR;
            case "CRITICAL": return CRITICAL;
            default: return INFO;
        }
    }
} 