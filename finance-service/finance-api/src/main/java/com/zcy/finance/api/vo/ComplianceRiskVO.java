package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ComplianceRiskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String scenario;
    private String overallLevel;
    private BigDecimal riskScore;
    private int totalRiskCount;
    private int highRiskCount;
    private List<RiskItem> riskItems = new ArrayList<RiskItem>();
    private List<String> advices = new ArrayList<String>();
    private List<String> nextActions = new ArrayList<String>();

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

    public String getOverallLevel() {
        return overallLevel;
    }

    public void setOverallLevel(String overallLevel) {
        this.overallLevel = overallLevel;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public int getTotalRiskCount() {
        return totalRiskCount;
    }

    public void setTotalRiskCount(int totalRiskCount) {
        this.totalRiskCount = totalRiskCount;
    }

    public int getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(int highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public List<RiskItem> getRiskItems() {
        return riskItems;
    }

    public void setRiskItems(List<RiskItem> riskItems) {
        this.riskItems = riskItems;
    }

    public List<String> getAdvices() {
        return advices;
    }

    public void setAdvices(List<String> advices) {
        this.advices = advices;
    }

    public List<String> getNextActions() {
        return nextActions;
    }

    public void setNextActions(List<String> nextActions) {
        this.nextActions = nextActions;
    }

    public static class RiskItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String riskId;
        private String module;
        private String severity;
        private String title;
        private String description;
        private String relatedDocNo;
        private BigDecimal amount;
        private String owner;
        private String dueDate;
        private String status;
        private String suggestion;

        public RiskItem() {
        }

        public RiskItem(String riskId, String module, String severity, String title, String description,
                        String relatedDocNo, BigDecimal amount, String owner, String dueDate,
                        String status, String suggestion) {
            this.riskId = riskId;
            this.module = module;
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.relatedDocNo = relatedDocNo;
            this.amount = amount;
            this.owner = owner;
            this.dueDate = dueDate;
            this.status = status;
            this.suggestion = suggestion;
        }

        public String getRiskId() {
            return riskId;
        }

        public void setRiskId(String riskId) {
            this.riskId = riskId;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getRelatedDocNo() {
            return relatedDocNo;
        }

        public void setRelatedDocNo(String relatedDocNo) {
            this.relatedDocNo = relatedDocNo;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getDueDate() {
            return dueDate;
        }

        public void setDueDate(String dueDate) {
            this.dueDate = dueDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }
}
