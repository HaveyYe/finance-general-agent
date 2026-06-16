package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AnomalyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String anomalyNo;
    private String period;
    private String anomalyType;
    private String metric;
    private String severity;
    private BigDecimal actualValue = BigDecimal.ZERO;
    private BigDecimal expectedValue = BigDecimal.ZERO;
    private BigDecimal deviationRate = BigDecimal.ZERO;
    private String description;
    private String suggestion;
    private List<String> evidence = new ArrayList<String>();

    public String getAnomalyNo() { return anomalyNo; }
    public void setAnomalyNo(String anomalyNo) { this.anomalyNo = anomalyNo; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public BigDecimal getActualValue() { return actualValue; }
    public void setActualValue(BigDecimal actualValue) { this.actualValue = actualValue; }
    public BigDecimal getExpectedValue() { return expectedValue; }
    public void setExpectedValue(BigDecimal expectedValue) { this.expectedValue = expectedValue; }
    public BigDecimal getDeviationRate() { return deviationRate; }
    public void setDeviationRate(BigDecimal deviationRate) { this.deviationRate = deviationRate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
}
