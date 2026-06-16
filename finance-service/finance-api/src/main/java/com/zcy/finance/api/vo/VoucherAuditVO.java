package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VoucherAuditVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String voucherNo;
    private String voucherDate;
    private String auditStatus;
    private String overallSeverity;
    private BigDecimal debitTotal = BigDecimal.ZERO;
    private BigDecimal creditTotal = BigDecimal.ZERO;
    private int redCount;
    private int yellowCount;
    private int blueCount;
    private List<AuditItem> auditItems = new ArrayList<AuditItem>();
    private List<String> advices = new ArrayList<String>();
    private List<String> nextActions = new ArrayList<String>();

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public String getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(String voucherDate) {
        this.voucherDate = voucherDate;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getOverallSeverity() {
        return overallSeverity;
    }

    public void setOverallSeverity(String overallSeverity) {
        this.overallSeverity = overallSeverity;
    }

    public BigDecimal getDebitTotal() {
        return debitTotal;
    }

    public void setDebitTotal(BigDecimal debitTotal) {
        this.debitTotal = debitTotal;
    }

    public BigDecimal getCreditTotal() {
        return creditTotal;
    }

    public void setCreditTotal(BigDecimal creditTotal) {
        this.creditTotal = creditTotal;
    }

    public int getRedCount() {
        return redCount;
    }

    public void setRedCount(int redCount) {
        this.redCount = redCount;
    }

    public int getYellowCount() {
        return yellowCount;
    }

    public void setYellowCount(int yellowCount) {
        this.yellowCount = yellowCount;
    }

    public int getBlueCount() {
        return blueCount;
    }

    public void setBlueCount(int blueCount) {
        this.blueCount = blueCount;
    }

    public List<AuditItem> getAuditItems() {
        return auditItems;
    }

    public void setAuditItems(List<AuditItem> auditItems) {
        this.auditItems = auditItems;
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

    public static class AuditItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String itemId;
        private String category;
        private String severity;
        private String title;
        private String description;
        private String suggestion;

        public AuditItem() {
        }

        public AuditItem(String itemId, String category, String severity, String title, String description, String suggestion) {
            this.itemId = itemId;
            this.category = category;
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.suggestion = suggestion;
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
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

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }
}
