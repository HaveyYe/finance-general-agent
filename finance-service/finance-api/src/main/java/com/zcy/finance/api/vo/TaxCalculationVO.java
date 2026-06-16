package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TaxCalculationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taxType;
    private String period;
    private BigDecimal taxableAmount;
    private BigDecimal taxRate;
    private BigDecimal deductibleAmount;
    private BigDecimal taxPayable;
    private List<String> calculationSteps = new ArrayList<String>();
    private List<String> riskHints = new ArrayList<String>();

    public String getTaxType() {
        return taxType;
    }

    public void setTaxType(String taxType) {
        this.taxType = taxType;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getDeductibleAmount() {
        return deductibleAmount;
    }

    public void setDeductibleAmount(BigDecimal deductibleAmount) {
        this.deductibleAmount = deductibleAmount;
    }

    public BigDecimal getTaxPayable() {
        return taxPayable;
    }

    public void setTaxPayable(BigDecimal taxPayable) {
        this.taxPayable = taxPayable;
    }

    public List<String> getCalculationSteps() {
        return calculationSteps;
    }

    public void setCalculationSteps(List<String> calculationSteps) {
        this.calculationSteps = calculationSteps;
    }

    public List<String> getRiskHints() {
        return riskHints;
    }

    public void setRiskHints(List<String> riskHints) {
        this.riskHints = riskHints;
    }
}
