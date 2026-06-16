package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CollectionPlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String planNo;
    private String period;
    private String customerName;
    private String creditLevel;
    private BigDecimal overdueAmount = BigDecimal.ZERO;
    private int overdueDays;
    private String collectionStrategy;
    private String escalationLevel;
    private String freezeCreditSuggestion;
    private String collectionLetterDraft;
    private List<ActionItem> actionItems = new ArrayList<ActionItem>();
    private List<String> advices = new ArrayList<String>();

    public String getPlanNo() {
        return planNo;
    }

    public void setPlanNo(String planNo) {
        this.planNo = planNo;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCreditLevel() {
        return creditLevel;
    }

    public void setCreditLevel(String creditLevel) {
        this.creditLevel = creditLevel;
    }

    public BigDecimal getOverdueAmount() {
        return overdueAmount;
    }

    public void setOverdueAmount(BigDecimal overdueAmount) {
        this.overdueAmount = overdueAmount;
    }

    public int getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(int overdueDays) {
        this.overdueDays = overdueDays;
    }

    public String getCollectionStrategy() {
        return collectionStrategy;
    }

    public void setCollectionStrategy(String collectionStrategy) {
        this.collectionStrategy = collectionStrategy;
    }

    public String getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(String escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public String getFreezeCreditSuggestion() {
        return freezeCreditSuggestion;
    }

    public void setFreezeCreditSuggestion(String freezeCreditSuggestion) {
        this.freezeCreditSuggestion = freezeCreditSuggestion;
    }

    public String getCollectionLetterDraft() {
        return collectionLetterDraft;
    }

    public void setCollectionLetterDraft(String collectionLetterDraft) {
        this.collectionLetterDraft = collectionLetterDraft;
    }

    public List<ActionItem> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<ActionItem> actionItems) {
        this.actionItems = actionItems;
    }

    public List<String> getAdvices() {
        return advices;
    }

    public void setAdvices(List<String> advices) {
        this.advices = advices;
    }

    public static class ActionItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String actionDate;
        private String actionType;
        private String owner;
        private String content;
        private String status;

        public ActionItem() {
        }

        public ActionItem(String actionDate, String actionType, String owner, String content, String status) {
            this.actionDate = actionDate;
            this.actionType = actionType;
            this.owner = owner;
            this.content = content;
            this.status = status;
        }

        public String getActionDate() {
            return actionDate;
        }

        public void setActionDate(String actionDate) {
            this.actionDate = actionDate;
        }

        public String getActionType() {
            return actionType;
        }

        public void setActionType(String actionType) {
            this.actionType = actionType;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
