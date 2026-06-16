package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VoucherVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String voucherNo;
    private String voucherDate;
    private String period;
    private String summary;
    private String status;
    private String preparer;
    private String reviewer;
    private String sourceDocumentNo;
    private BigDecimal debitTotal;
    private BigDecimal creditTotal;
    private List<VoucherEntry> entries = new ArrayList<VoucherEntry>();
    private List<String> auditMessages = new ArrayList<String>();

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

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPreparer() {
        return preparer;
    }

    public void setPreparer(String preparer) {
        this.preparer = preparer;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getSourceDocumentNo() {
        return sourceDocumentNo;
    }

    public void setSourceDocumentNo(String sourceDocumentNo) {
        this.sourceDocumentNo = sourceDocumentNo;
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

    public List<VoucherEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<VoucherEntry> entries) {
        this.entries = entries;
    }

    public List<String> getAuditMessages() {
        return auditMessages;
    }

    public void setAuditMessages(List<String> auditMessages) {
        this.auditMessages = auditMessages;
    }

    public static class VoucherEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        private String accountCode;
        private String accountName;
        private String direction;
        private BigDecimal debitAmount = BigDecimal.ZERO;
        private BigDecimal creditAmount = BigDecimal.ZERO;
        private String remark;

        public VoucherEntry() {
        }

        public VoucherEntry(String accountCode, String accountName, String direction, BigDecimal debitAmount, BigDecimal creditAmount, String remark) {
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.direction = direction;
            this.debitAmount = debitAmount;
            this.creditAmount = creditAmount;
            this.remark = remark;
        }

        public String getAccountCode() { return accountCode; }
        public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public BigDecimal getDebitAmount() { return debitAmount; }
        public void setDebitAmount(BigDecimal debitAmount) { this.debitAmount = debitAmount; }
        public BigDecimal getCreditAmount() { return creditAmount; }
        public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
