package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VoucherCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String voucherDate;
    private String summary;
    private List<Entry> entries = new ArrayList<Entry>();

    public String getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(String voucherDate) {
        this.voucherDate = voucherDate;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;

        private String accountCode;
        private String accountName;
        private BigDecimal debitAmount = BigDecimal.ZERO;
        private BigDecimal creditAmount = BigDecimal.ZERO;

        public String getAccountCode() {
            return accountCode;
        }

        public void setAccountCode(String accountCode) {
            this.accountCode = accountCode;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public BigDecimal getDebitAmount() {
            return debitAmount;
        }

        public void setDebitAmount(BigDecimal debitAmount) {
            this.debitAmount = debitAmount;
        }

        public BigDecimal getCreditAmount() {
            return creditAmount;
        }

        public void setCreditAmount(BigDecimal creditAmount) {
            this.creditAmount = creditAmount;
        }
    }
}
