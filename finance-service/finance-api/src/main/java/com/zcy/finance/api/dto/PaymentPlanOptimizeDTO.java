package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class PaymentPlanOptimizeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String supplierName;
    private BigDecimal payableAmount;
    private String dueDate;
    private String discountTerm;
    private BigDecimal cashBalance;
    private BigDecimal safetyCashLevel;
    private boolean allowMergePayment = true;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getDiscountTerm() {
        return discountTerm;
    }

    public void setDiscountTerm(String discountTerm) {
        this.discountTerm = discountTerm;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public BigDecimal getSafetyCashLevel() {
        return safetyCashLevel;
    }

    public void setSafetyCashLevel(BigDecimal safetyCashLevel) {
        this.safetyCashLevel = safetyCashLevel;
    }

    public boolean isAllowMergePayment() {
        return allowMergePayment;
    }

    public void setAllowMergePayment(boolean allowMergePayment) {
        this.allowMergePayment = allowMergePayment;
    }
}
