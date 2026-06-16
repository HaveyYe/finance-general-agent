package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class CounterpartyReconcileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String counterpartyName;
    private String counterpartyType;
    private BigDecimal internalBalance;
    private BigDecimal counterpartyBalance;
    private Boolean includeConfirmationLetter;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    public String getCounterpartyType() {
        return counterpartyType;
    }

    public void setCounterpartyType(String counterpartyType) {
        this.counterpartyType = counterpartyType;
    }

    public BigDecimal getInternalBalance() {
        return internalBalance;
    }

    public void setInternalBalance(BigDecimal internalBalance) {
        this.internalBalance = internalBalance;
    }

    public BigDecimal getCounterpartyBalance() {
        return counterpartyBalance;
    }

    public void setCounterpartyBalance(BigDecimal counterpartyBalance) {
        this.counterpartyBalance = counterpartyBalance;
    }

    public Boolean getIncludeConfirmationLetter() {
        return includeConfirmationLetter;
    }

    public void setIncludeConfirmationLetter(Boolean includeConfirmationLetter) {
        this.includeConfirmationLetter = includeConfirmationLetter;
    }
}
