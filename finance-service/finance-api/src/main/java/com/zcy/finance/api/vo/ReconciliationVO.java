package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reconciliationNo;
    private String partnerId;
    private String partnerName;
    private String partnerType;
    private String period;
    private BigDecimal internalBalance = BigDecimal.ZERO;
    private BigDecimal partnerBalance = BigDecimal.ZERO;
    private BigDecimal differenceAmount = BigDecimal.ZERO;
    private String status;
    private String conclusion;
    private List<DifferenceItem> differenceItems = new ArrayList<DifferenceItem>();

    public String getReconciliationNo() { return reconciliationNo; }
    public void setReconciliationNo(String reconciliationNo) { this.reconciliationNo = reconciliationNo; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
    public String getPartnerType() { return partnerType; }
    public void setPartnerType(String partnerType) { this.partnerType = partnerType; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public BigDecimal getInternalBalance() { return internalBalance; }
    public void setInternalBalance(BigDecimal internalBalance) { this.internalBalance = internalBalance; }
    public BigDecimal getPartnerBalance() { return partnerBalance; }
    public void setPartnerBalance(BigDecimal partnerBalance) { this.partnerBalance = partnerBalance; }
    public BigDecimal getDifferenceAmount() { return differenceAmount; }
    public void setDifferenceAmount(BigDecimal differenceAmount) { this.differenceAmount = differenceAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public List<DifferenceItem> getDifferenceItems() { return differenceItems; }
    public void setDifferenceItems(List<DifferenceItem> differenceItems) { this.differenceItems = differenceItems; }

    public static class DifferenceItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String documentNo;
        private String differenceType;
        private BigDecimal amount = BigDecimal.ZERO;
        private String reason;
        private String suggestion;

        public DifferenceItem() {
        }

        public DifferenceItem(String documentNo, String differenceType, BigDecimal amount, String reason, String suggestion) {
            this.documentNo = documentNo;
            this.differenceType = differenceType;
            this.amount = amount;
            this.reason = reason;
            this.suggestion = suggestion;
        }

        public String getDocumentNo() { return documentNo; }
        public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }
        public String getDifferenceType() { return differenceType; }
        public void setDifferenceType(String differenceType) { this.differenceType = differenceType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
}
