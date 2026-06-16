package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VarianceDiagnosisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String diagnosisNo;
    private String period;
    private String metricName;
    private String department;
    private BigDecimal actualAmount = BigDecimal.ZERO;
    private BigDecimal budgetAmount = BigDecimal.ZERO;
    private BigDecimal previousAmount = BigDecimal.ZERO;
    private BigDecimal budgetVariance = BigDecimal.ZERO;
    private BigDecimal budgetVarianceRate = BigDecimal.ZERO;
    private BigDecimal momVariance = BigDecimal.ZERO;
    private BigDecimal momVarianceRate = BigDecimal.ZERO;
    private String severity;
    private String conclusion;
    private List<DriverItem> drivers = new ArrayList<DriverItem>();
    private List<String> evidence = new ArrayList<String>();
    private List<String> actionPlan = new ArrayList<String>();
    private List<String> followUpQuestions = new ArrayList<String>();

    public String getDiagnosisNo() { return diagnosisNo; }
    public void setDiagnosisNo(String diagnosisNo) { this.diagnosisNo = diagnosisNo; }
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
    public BigDecimal getBudgetVariance() { return budgetVariance; }
    public void setBudgetVariance(BigDecimal budgetVariance) { this.budgetVariance = budgetVariance; }
    public BigDecimal getBudgetVarianceRate() { return budgetVarianceRate; }
    public void setBudgetVarianceRate(BigDecimal budgetVarianceRate) { this.budgetVarianceRate = budgetVarianceRate; }
    public BigDecimal getMomVariance() { return momVariance; }
    public void setMomVariance(BigDecimal momVariance) { this.momVariance = momVariance; }
    public BigDecimal getMomVarianceRate() { return momVarianceRate; }
    public void setMomVarianceRate(BigDecimal momVarianceRate) { this.momVarianceRate = momVarianceRate; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public List<DriverItem> getDrivers() { return drivers; }
    public void setDrivers(List<DriverItem> drivers) { this.drivers = drivers; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
    public List<String> getActionPlan() { return actionPlan; }
    public void setActionPlan(List<String> actionPlan) { this.actionPlan = actionPlan; }
    public List<String> getFollowUpQuestions() { return followUpQuestions; }
    public void setFollowUpQuestions(List<String> followUpQuestions) { this.followUpQuestions = followUpQuestions; }

    public static class DriverItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String driverName;
        private BigDecimal impactAmount;
        private BigDecimal contributionRate;
        private String owner;
        private String explanation;

        public DriverItem() {
        }

        public DriverItem(String driverName, BigDecimal impactAmount, BigDecimal contributionRate, String owner, String explanation) {
            this.driverName = driverName;
            this.impactAmount = impactAmount;
            this.contributionRate = contributionRate;
            this.owner = owner;
            this.explanation = explanation;
        }

        public String getDriverName() { return driverName; }
        public void setDriverName(String driverName) { this.driverName = driverName; }
        public BigDecimal getImpactAmount() { return impactAmount; }
        public void setImpactAmount(BigDecimal impactAmount) { this.impactAmount = impactAmount; }
        public BigDecimal getContributionRate() { return contributionRate; }
        public void setContributionRate(BigDecimal contributionRate) { this.contributionRate = contributionRate; }
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }
}
