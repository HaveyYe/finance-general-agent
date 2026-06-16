package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;

public class FinancialRatioVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String name;
    private String category;
    private BigDecimal value = BigDecimal.ZERO;
    private String unit;
    private String formula;
    private String referenceRange;
    private String evaluation;
    private String healthLevel;
    private String description;

    public FinancialRatioVO() {
    }

    public FinancialRatioVO(String period, String name, String category, BigDecimal value, String unit,
                            String formula, String referenceRange, String evaluation, String healthLevel,
                            String description) {
        this.period = period;
        this.name = name;
        this.category = category;
        this.value = value;
        this.unit = unit;
        this.formula = formula;
        this.referenceRange = referenceRange;
        this.evaluation = evaluation;
        this.healthLevel = healthLevel;
        this.description = description;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
    public String getEvaluation() { return evaluation; }
    public void setEvaluation(String evaluation) { this.evaluation = evaluation; }
    public String getHealthLevel() { return healthLevel; }
    public void setHealthLevel(String healthLevel) { this.healthLevel = healthLevel; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
