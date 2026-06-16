package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CounterpartyReconcileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reconcileNo;
    private String period;
    private String counterpartyName;
    private String counterpartyType;
    private BigDecimal internalBalance = BigDecimal.ZERO;
    private BigDecimal counterpartyBalance = BigDecimal.ZERO;
    private BigDecimal differenceAmount = BigDecimal.ZERO;
    private String reconcileStatus;
    private String riskLevel;
    private String confirmationStatus;
    private String confirmationLetterDraft;
    private List<DifferenceItem> differenceItems = new ArrayList<DifferenceItem>();
    private List<String> advices = new ArrayList<String>();
    private List<String> nextActions = new ArrayList<String>();

    public String getReconcileNo() { return reconcileNo; }
    public void setReconcileNo(String reconcileNo) { this.reconcileNo = reconcileNo; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    public String getCounterpartyType() { return counterpartyType; }
    public void setCounterpartyType(String counterpartyType) { this.counterpartyType = counterpartyType; }
    public BigDecimal getInternalBalance() { return internalBalance; }
    public void setInternalBalance(BigDecimal internalBalance) { this.internalBalance = internalBalance; }
    public BigDecimal getCounterpartyBalance() { return counterpartyBalance; }
    public void setCounterpartyBalance(BigDecimal counterpartyBalance) { this.counterpartyBalance = counterpartyBalance; }
    public BigDecimal getDifferenceAmount() { return differenceAmount; }
    public void setDifferenceAmount(BigDecimal differenceAmount) { this.differenceAmount = differenceAmount; }
    public String getReconcileStatus() { return reconcileStatus; }
    public void setReconcileStatus(String reconcileStatus) { this.reconcileStatus = reconcileStatus; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getConfirmationStatus() { return confirmationStatus; }
    public void setConfirmationStatus(String confirmationStatus) { this.confirmationStatus = confirmationStatus; }
    public String getConfirmationLetterDraft() { return confirmationLetterDraft; }
    public void setConfirmationLetterDraft(String confirmationLetterDraft) { this.confirmationLetterDraft = confirmationLetterDraft; }
    public List<DifferenceItem> getDifferenceItems() { return differenceItems; }
    public void setDifferenceItems(List<DifferenceItem> differenceItems) { this.differenceItems = differenceItems; }
    public List<String> getAdvices() { return advices; }
    public void setAdvices(List<String> advices) { this.advices = advices; }
    public List<String> getNextActions() { return nextActions; }
    public void setNextActions(List<String> nextActions) { this.nextActions = nextActions; }

    public static class DifferenceItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String itemNo;
        private String differenceType;
        private BigDecimal amount;
        private String reason;
        private String owner;
        private String suggestion;

        public DifferenceItem() {
        }

        public DifferenceItem(String itemNo, String differenceType, BigDecimal amount, String reason, String owner, String suggestion) {
            this.itemNo = itemNo;
            this.differenceType = differenceType;
            this.amount = amount;
            this.reason = reason;
            this.owner = owner;
            this.suggestion = suggestion;
        }

        public String getItemNo() { return itemNo; }
        public void setItemNo(String itemNo) { this.itemNo = itemNo; }
        public String getDifferenceType() { return differenceType; }
        public void setDifferenceType(String differenceType) { this.differenceType = differenceType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
}
