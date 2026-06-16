package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaymentPlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String planNo;
    private String period;
    private String supplierName;
    private BigDecimal payableAmount = BigDecimal.ZERO;
    private String recommendedPayDate;
    private String paymentStrategy;
    private BigDecimal discountBenefit = BigDecimal.ZERO;
    private BigDecimal cashAfterPayment = BigDecimal.ZERO;
    private String liquidityImpact;
    private boolean mergePaymentRecommended;
    private List<String> scheduleItems = new ArrayList<String>();
    private List<String> riskHints = new ArrayList<String>();
    private List<String> nextActions = new ArrayList<String>();

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

    public String getRecommendedPayDate() {
        return recommendedPayDate;
    }

    public void setRecommendedPayDate(String recommendedPayDate) {
        this.recommendedPayDate = recommendedPayDate;
    }

    public String getPaymentStrategy() {
        return paymentStrategy;
    }

    public void setPaymentStrategy(String paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public BigDecimal getDiscountBenefit() {
        return discountBenefit;
    }

    public void setDiscountBenefit(BigDecimal discountBenefit) {
        this.discountBenefit = discountBenefit;
    }

    public BigDecimal getCashAfterPayment() {
        return cashAfterPayment;
    }

    public void setCashAfterPayment(BigDecimal cashAfterPayment) {
        this.cashAfterPayment = cashAfterPayment;
    }

    public String getLiquidityImpact() {
        return liquidityImpact;
    }

    public void setLiquidityImpact(String liquidityImpact) {
        this.liquidityImpact = liquidityImpact;
    }

    public boolean isMergePaymentRecommended() {
        return mergePaymentRecommended;
    }

    public void setMergePaymentRecommended(boolean mergePaymentRecommended) {
        this.mergePaymentRecommended = mergePaymentRecommended;
    }

    public List<String> getScheduleItems() {
        return scheduleItems;
    }

    public void setScheduleItems(List<String> scheduleItems) {
        this.scheduleItems = scheduleItems;
    }

    public List<String> getRiskHints() {
        return riskHints;
    }

    public void setRiskHints(List<String> riskHints) {
        this.riskHints = riskHints;
    }

    public List<String> getNextActions() {
        return nextActions;
    }

    public void setNextActions(List<String> nextActions) {
        this.nextActions = nextActions;
    }
}
