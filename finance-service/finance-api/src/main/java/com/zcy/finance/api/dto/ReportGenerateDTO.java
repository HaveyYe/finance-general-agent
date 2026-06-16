package com.zcy.finance.api.dto;

import java.io.Serializable;

public class ReportGenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportType;
    private String period;

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
