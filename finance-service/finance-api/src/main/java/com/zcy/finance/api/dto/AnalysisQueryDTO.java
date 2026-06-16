package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AnalysisQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> metrics = new ArrayList<String>();
    private String period;

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
