package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BudgetControlVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String budgetNo;
    private String period;
    private String department;
    private String projectCode;
    private String expenseType;
    private BigDecimal annualBudget;
    private BigDecimal periodBudget;
    private BigDecimal actualUsed;
    private BigDecimal committedAmount;
    private BigDecimal requestedAmount;
    private BigDecimal availableBudget;
    private BigDecimal executionRate;
    private BigDecimal forecastAmount;
    private BigDecimal forecastVariance;
    private String controlStatus;
    private String riskLevel;
    private String approvalSuggestion;
    private List<ControlItem> controlItems = new ArrayList<ControlItem>();
    private List<String> warnings = new ArrayList<String>();
    private List<String> nextActions = new ArrayList<String>();

    public String getBudgetNo() {
        return budgetNo;
    }

    public void setBudgetNo(String budgetNo) {
        this.budgetNo = budgetNo;
    }

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

    public BigDecimal getAnnualBudget() {
        return annualBudget;
    }

    public void setAnnualBudget(BigDecimal annualBudget) {
        this.annualBudget = annualBudget;
    }

    public BigDecimal getPeriodBudget() {
        return periodBudget;
    }

    public void setPeriodBudget(BigDecimal periodBudget) {
        this.periodBudget = periodBudget;
    }

    public BigDecimal getActualUsed() {
        return actualUsed;
    }

    public void setActualUsed(BigDecimal actualUsed) {
        this.actualUsed = actualUsed;
    }

    public BigDecimal getCommittedAmount() {
        return committedAmount;
    }

    public void setCommittedAmount(BigDecimal committedAmount) {
        this.committedAmount = committedAmount;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getAvailableBudget() {
        return availableBudget;
    }

    public void setAvailableBudget(BigDecimal availableBudget) {
        this.availableBudget = availableBudget;
    }

    public BigDecimal getExecutionRate() {
        return executionRate;
    }

    public void setExecutionRate(BigDecimal executionRate) {
        this.executionRate = executionRate;
    }

    public BigDecimal getForecastAmount() {
        return forecastAmount;
    }

    public void setForecastAmount(BigDecimal forecastAmount) {
        this.forecastAmount = forecastAmount;
    }

    public BigDecimal getForecastVariance() {
        return forecastVariance;
    }

    public void setForecastVariance(BigDecimal forecastVariance) {
        this.forecastVariance = forecastVariance;
    }

    public String getControlStatus() {
        return controlStatus;
    }

    public void setControlStatus(String controlStatus) {
        this.controlStatus = controlStatus;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getApprovalSuggestion() {
        return approvalSuggestion;
    }

    public void setApprovalSuggestion(String approvalSuggestion) {
        this.approvalSuggestion = approvalSuggestion;
    }

    public List<ControlItem> getControlItems() {
        return controlItems;
    }

    public void setControlItems(List<ControlItem> controlItems) {
        this.controlItems = controlItems;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getNextActions() {
        return nextActions;
    }

    public void setNextActions(List<String> nextActions) {
        this.nextActions = nextActions;
    }

    public static class ControlItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String itemCode;
        private String itemName;
        private String status;
        private String conclusion;
        private String suggestion;

        public ControlItem() {
        }

        public ControlItem(String itemCode, String itemName, String status, String conclusion, String suggestion) {
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.status = status;
            this.conclusion = conclusion;
            this.suggestion = suggestion;
        }

        public String getItemCode() {
            return itemCode;
        }

        public void setItemCode(String itemCode) {
            this.itemCode = itemCode;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getConclusion() {
            return conclusion;
        }

        public void setConclusion(String conclusion) {
            this.conclusion = conclusion;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }
}
