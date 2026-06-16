package com.zcy.finance.api.dto;

import java.io.Serializable;

public class DataIntegrationQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String systemType;
    private String syncMode;
    private Boolean includeQualityDetails;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public String getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(String syncMode) {
        this.syncMode = syncMode;
    }

    public Boolean getIncludeQualityDetails() {
        return includeQualityDetails;
    }

    public void setIncludeQualityDetails(Boolean includeQualityDetails) {
        this.includeQualityDetails = includeQualityDetails;
    }
}
