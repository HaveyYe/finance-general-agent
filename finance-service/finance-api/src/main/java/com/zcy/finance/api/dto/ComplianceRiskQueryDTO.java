package com.zcy.finance.api.dto;

import java.io.Serializable;

public class ComplianceRiskQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String scenario;
    private String minSeverity;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getMinSeverity() {
        return minSeverity;
    }

    public void setMinSeverity(String minSeverity) {
        this.minSeverity = minSeverity;
    }
}
