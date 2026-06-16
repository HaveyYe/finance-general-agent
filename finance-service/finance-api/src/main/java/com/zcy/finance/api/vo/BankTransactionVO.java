package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;

public class BankTransactionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String transactionNo;
    private String accountNo;
    private String transactionDate;
    private String counterparty;
    private String summary;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal balance;
    private String status;
    private String matchedVoucherNo;
    private String riskHint;

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMatchedVoucherNo() {
        return matchedVoucherNo;
    }

    public void setMatchedVoucherNo(String matchedVoucherNo) {
        this.matchedVoucherNo = matchedVoucherNo;
    }

    public String getRiskHint() {
        return riskHint;
    }

    public void setRiskHint(String riskHint) {
        this.riskHint = riskHint;
    }
}
