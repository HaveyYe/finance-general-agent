package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class BudgetControlQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String department;
    private String projectCode;
    private String expenseType;
    private BigDecimal requestedAmount;
    private String scenario;
    private Boolean includeForecast;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public Boolean getIncludeForecast() {
        return includeForecast;
    }

    public void setIncludeForecast(Boolean includeForecast) {
        this.includeForecast = includeForecast;
    }
}
