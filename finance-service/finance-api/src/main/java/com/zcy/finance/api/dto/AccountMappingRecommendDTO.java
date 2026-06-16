package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class AccountMappingRecommendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String documentType;
    private String businessScenario;
    private String summary;
    private String counterpartyName;
    private String department;
    private String projectCode;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private Boolean includeVoucherPreview;

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getBusinessScenario() {
        return businessScenario;
    }

    public void setBusinessScenario(String businessScenario) {
        this.businessScenario = businessScenario;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Boolean getIncludeVoucherPreview() {
        return includeVoucherPreview;
    }

    public void setIncludeVoucherPreview(Boolean includeVoucherPreview) {
        this.includeVoucherPreview = includeVoucherPreview;
    }
}
