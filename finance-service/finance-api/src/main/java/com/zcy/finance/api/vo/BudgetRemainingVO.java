package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;

public class BudgetRemainingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String budgetNo;
    private String department;
    private String period;
    private BigDecimal budgetAmount = BigDecimal.ZERO;
    private BigDecimal usedAmount = BigDecimal.ZERO;
    private BigDecimal occupiedAmount = BigDecimal.ZERO;
    private BigDecimal remainingAmount = BigDecimal.ZERO;
    private BigDecimal executionRate = BigDecimal.ZERO;
    private String riskLevel;
    private String suggestion;

    public String getBudgetNo() { return budgetNo; }
    public void setBudgetNo(String budgetNo) { this.budgetNo = budgetNo; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
    public BigDecimal getUsedAmount() { return usedAmount; }
    public void setUsedAmount(BigDecimal usedAmount) { this.usedAmount = usedAmount; }
    public BigDecimal getOccupiedAmount() { return occupiedAmount; }
    public void setOccupiedAmount(BigDecimal occupiedAmount) { this.occupiedAmount = occupiedAmount; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
    public BigDecimal getExecutionRate() { return executionRate; }
    public void setExecutionRate(BigDecimal executionRate) { this.executionRate = executionRate; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
}
