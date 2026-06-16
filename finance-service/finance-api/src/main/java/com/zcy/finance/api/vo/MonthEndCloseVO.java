package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MonthEndCloseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String closeNo;
    private String period;
    private String entityName;
    private String closeType;
    private String closeStatus;
    private BigDecimal progressRate;
    private Integer totalTaskCount;
    private Integer completedTaskCount;
    private Integer blockerCount;
    private Integer warningCount;
    private Boolean readyToClose;
    private String estimatedCloseDate;
    private List<CloseChecklistItem> checklist = new ArrayList<CloseChecklistItem>();
    private List<String> blockers = new ArrayList<String>();
    private List<String> warnings = new ArrayList<String>();
    private List<String> nextActions = new ArrayList<String>();

    public String getCloseNo() {
        return closeNo;
    }

    public void setCloseNo(String closeNo) {
        this.closeNo = closeNo;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getCloseType() {
        return closeType;
    }

    public void setCloseType(String closeType) {
        this.closeType = closeType;
    }

    public String getCloseStatus() {
        return closeStatus;
    }

    public void setCloseStatus(String closeStatus) {
        this.closeStatus = closeStatus;
    }

    public BigDecimal getProgressRate() {
        return progressRate;
    }

    public void setProgressRate(BigDecimal progressRate) {
        this.progressRate = progressRate;
    }

    public Integer getTotalTaskCount() {
        return totalTaskCount;
    }

    public void setTotalTaskCount(Integer totalTaskCount) {
        this.totalTaskCount = totalTaskCount;
    }

    public Integer getCompletedTaskCount() {
        return completedTaskCount;
    }

    public void setCompletedTaskCount(Integer completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }

    public Integer getBlockerCount() {
        return blockerCount;
    }

    public void setBlockerCount(Integer blockerCount) {
        this.blockerCount = blockerCount;
    }

    public Integer getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Integer warningCount) {
        this.warningCount = warningCount;
    }

    public Boolean getReadyToClose() {
        return readyToClose;
    }

    public void setReadyToClose(Boolean readyToClose) {
        this.readyToClose = readyToClose;
    }

    public String getEstimatedCloseDate() {
        return estimatedCloseDate;
    }

    public void setEstimatedCloseDate(String estimatedCloseDate) {
        this.estimatedCloseDate = estimatedCloseDate;
    }

    public List<CloseChecklistItem> getChecklist() {
        return checklist;
    }

    public void setChecklist(List<CloseChecklistItem> checklist) {
        this.checklist = checklist;
    }

    public List<String> getBlockers() {
        return blockers;
    }

    public void setBlockers(List<String> blockers) {
        this.blockers = blockers;
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

    public static class CloseChecklistItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String module;
        private String itemCode;
        private String itemName;
        private String status;
        private String owner;
        private String dueDate;
        private String evidence;
        private String suggestion;

        public CloseChecklistItem() {
        }

        public CloseChecklistItem(String module, String itemCode, String itemName, String status,
                                  String owner, String dueDate, String evidence, String suggestion) {
            this.module = module;
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.status = status;
            this.owner = owner;
            this.dueDate = dueDate;
            this.evidence = evidence;
            this.suggestion = suggestion;
        }

        public String getModule() { return module; }
        public void setModule(String module) { this.module = module; }
        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
        public String getEvidence() { return evidence; }
        public void setEvidence(String evidence) { this.evidence = evidence; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
}
