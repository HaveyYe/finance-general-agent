package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AnalysisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private List<Metric> metrics = new ArrayList<Metric>();
    private List<String> anomalies = new ArrayList<String>();
    private List<String> advices = new ArrayList<String>();

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    public List<String> getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(List<String> anomalies) {
        this.anomalies = anomalies;
    }

    public List<String> getAdvices() {
        return advices;
    }

    public void setAdvices(List<String> advices) {
        this.advices = advices;
    }

    public static class Metric implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private BigDecimal value;
        private String unit;
        private String evaluation;

        public Metric() {
        }

        public Metric(String name, BigDecimal value, String unit, String evaluation) {
            this.name = name;
            this.value = value;
            this.unit = unit;
            this.evaluation = evaluation;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getEvaluation() {
            return evaluation;
        }

        public void setEvaluation(String evaluation) {
            this.evaluation = evaluation;
        }
    }
}
