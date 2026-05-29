package com.devgomes.glpi_integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "glpi.sync")
public class GlpiSyncProperties {

    private boolean enabled = false;

    private long delayMs = 500;

    private String inputPath = "";

    /** Range GLPI para carregar índices de lookup (computers, users, states). */
    private String lookupRange = "0-9999";

    private long scheduleDelayMs = 3_600_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public boolean hasInputPath() {
        return inputPath != null && !inputPath.isBlank();
    }

    public String getLookupRange() {
        return lookupRange == null || lookupRange.isBlank() ? "0-9999" : lookupRange;
    }

    public void setLookupRange(String lookupRange) {
        this.lookupRange = lookupRange;
    }

    public long getScheduleDelayMs() {
        return scheduleDelayMs;
    }

    public void setScheduleDelayMs(long scheduleDelayMs) {
        this.scheduleDelayMs = scheduleDelayMs;
    }
}
