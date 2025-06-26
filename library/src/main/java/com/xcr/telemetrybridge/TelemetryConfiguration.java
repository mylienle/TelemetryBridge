package com.xcr.telemetrybridge;

import java.util.HashSet;
import java.util.Set;

public class TelemetryConfiguration {
    private String connectionString;
    private String instrumentationKey;
    private String cloudRoleName;
    private String cloudRoleInstance;
    private String appVersion;
    private int timeoutSeconds = 30;
    private double samplingRatio = 1.0;
    private Set<String> sensitiveKeys = new HashSet<>();

    public String getConnectionString() {
        return connectionString;
    }
    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getInstrumentationKey() {
        return instrumentationKey;
    }
    public void setInstrumentationKey(String instrumentationKey) {
        this.instrumentationKey = instrumentationKey;
    }

    public String getCloudRoleName() {
        return cloudRoleName;
    }
    public void setCloudRoleName(String cloudRoleName) {
        this.cloudRoleName = cloudRoleName;
    }

    public String getCloudRoleInstance() {
        return cloudRoleInstance;
    }
    public void setCloudRoleInstance(String cloudRoleInstance) {
        this.cloudRoleInstance = cloudRoleInstance;
    }

    public String getAppVersion() {
        return appVersion;
    }
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public double getSamplingRatio() {
        return samplingRatio;
    }
    public void setSamplingRatio(double samplingRatio) {
        this.samplingRatio = samplingRatio;
    }

    public Set<String> getSensitiveKeys() {
        return sensitiveKeys;
    }
    public void setSensitiveKeys(Set<String> sensitiveKeys) {
        this.sensitiveKeys = sensitiveKeys;
    }
} 