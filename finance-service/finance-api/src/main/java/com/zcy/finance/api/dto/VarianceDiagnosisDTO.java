package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class VarianceDiagnosisDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String metricName;
    private String department;
    private BigDecimal actualAmount;
    private BigDecimal budgetAmount;
    private BigDecimal previousAmount;
    private Boolean includeActionPlan;

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
    public BigDecimal getPreviousAmount() { return previousAmount; }
    public void setPreviousAmount(BigDecimal previousAmount) { this.previousAmount = previousAmount; }
    public Boolean getIncludeActionPlan() { return includeActionPlan; }
    public void setIncludeActionPlan(Boolean includeActionPlan) { this.includeActionPlan = includeActionPlan; }
}
