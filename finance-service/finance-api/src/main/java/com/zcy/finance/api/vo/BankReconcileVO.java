package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BankReconcileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountNo;
    private String period;
    private BigDecimal bankBalance;
    private BigDecimal ledgerBalance;
    private BigDecimal differenceAmount;
    private int totalCount;
    private int matchedCount;
    private int unmatchedCount;
    private List<BankTransactionVO> unmatchedTransactions = new ArrayList<BankTransactionVO>();
    private List<String> advices = new ArrayList<String>();

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(BigDecimal bankBalance) {
        this.bankBalance = bankBalance;
    }

    public BigDecimal getLedgerBalance() {
        return ledgerBalance;
    }

    public void setLedgerBalance(BigDecimal ledgerBalance) {
        this.ledgerBalance = ledgerBalance;
    }

    public BigDecimal getDifferenceAmount() {
        return differenceAmount;
    }

    public void setDifferenceAmount(BigDecimal differenceAmount) {
        this.differenceAmount = differenceAmount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(int matchedCount) {
        this.matchedCount = matchedCount;
    }

    public int getUnmatchedCount() {
        return unmatchedCount;
    }

    public void setUnmatchedCount(int unmatchedCount) {
        this.unmatchedCount = unmatchedCount;
    }

    public List<BankTransactionVO> getUnmatchedTransactions() {
        return unmatchedTransactions;
    }

    public void setUnmatchedTransactions(List<BankTransactionVO> unmatchedTransactions) {
        this.unmatchedTransactions = unmatchedTransactions;
    }

    public List<String> getAdvices() {
        return advices;
    }

    public void setAdvices(List<String> advices) {
        this.advices = advices;
    }
}
