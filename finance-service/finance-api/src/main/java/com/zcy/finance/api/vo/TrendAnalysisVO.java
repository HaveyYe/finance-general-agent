package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TrendAnalysisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String metric;
    private String startPeriod;
    private String endPeriod;
    private String unit;
    private String trendDirection;
    private BigDecimal changeRate = BigDecimal.ZERO;
    private String description;
    private List<DataPoint> dataPoints = new ArrayList<DataPoint>();
    private List<String> insights = new ArrayList<String>();

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public String getStartPeriod() { return startPeriod; }
    public void setStartPeriod(String startPeriod) { this.startPeriod = startPeriod; }
    public String getEndPeriod() { return endPeriod; }
    public void setEndPeriod(String endPeriod) { this.endPeriod = endPeriod; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getTrendDirection() { return trendDirection; }
    public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }
    public BigDecimal getChangeRate() { return changeRate; }
    public void setChangeRate(BigDecimal changeRate) { this.changeRate = changeRate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<DataPoint> getDataPoints() { return dataPoints; }
    public void setDataPoints(List<DataPoint> dataPoints) { this.dataPoints = dataPoints; }
    public List<String> getInsights() { return insights; }
    public void setInsights(List<String> insights) { this.insights = insights; }

    public static class DataPoint implements Serializable {
        private static final long serialVersionUID = 1L;

        private String period;
        private BigDecimal value = BigDecimal.ZERO;
        private BigDecimal changeRate = BigDecimal.ZERO;

        public DataPoint() {
        }

        public DataPoint(String period, BigDecimal value, BigDecimal changeRate) {
            this.period = period;
            this.value = value;
            this.changeRate = changeRate;
        }

        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public BigDecimal getChangeRate() { return changeRate; }
        public void setChangeRate(BigDecimal changeRate) { this.changeRate = changeRate; }
    }
}
