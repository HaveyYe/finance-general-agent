package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class CollectionPlanDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String customerName;
    private String creditLevel;
    private BigDecimal overdueAmount;
    private int overdueDays;
    private String owner;
    private boolean includeLetter = true;

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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isIncludeLetter() {
        return includeLetter;
    }

    public void setIncludeLetter(boolean includeLetter) {
        this.includeLetter = includeLetter;
    }
}
