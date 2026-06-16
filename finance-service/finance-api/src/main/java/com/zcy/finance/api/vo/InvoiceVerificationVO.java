package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InvoiceVerificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String invoiceNo;
    private String invoiceType;
    private String verifyStatus;
    private String taxAuthorityStatus;
    private boolean taxAuthorityMatched;
    private boolean duplicate;
    private List<String> duplicateRefs = new ArrayList<String>();
    private boolean serialRisk;
    private boolean deductible;
    private BigDecimal deductibleTaxAmount = BigDecimal.ZERO;
    private String archiveStatus;
    private String riskLevel;
    private List<String> riskHints = new ArrayList<String>();
    private List<String> nextActions = new ArrayList<String>();

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getVerifyStatus() {
        return verifyStatus;
    }

    public void setVerifyStatus(String verifyStatus) {
        this.verifyStatus = verifyStatus;
    }

    public String getTaxAuthorityStatus() {
        return taxAuthorityStatus;
    }

    public void setTaxAuthorityStatus(String taxAuthorityStatus) {
        this.taxAuthorityStatus = taxAuthorityStatus;
    }

    public boolean isTaxAuthorityMatched() {
        return taxAuthorityMatched;
    }

    public void setTaxAuthorityMatched(boolean taxAuthorityMatched) {
        this.taxAuthorityMatched = taxAuthorityMatched;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public List<String> getDuplicateRefs() {
        return duplicateRefs;
    }

    public void setDuplicateRefs(List<String> duplicateRefs) {
        this.duplicateRefs = duplicateRefs;
    }

    public boolean isSerialRisk() {
        return serialRisk;
    }

    public void setSerialRisk(boolean serialRisk) {
        this.serialRisk = serialRisk;
    }

    public boolean isDeductible() {
        return deductible;
    }

    public void setDeductible(boolean deductible) {
        this.deductible = deductible;
    }

    public BigDecimal getDeductibleTaxAmount() {
        return deductibleTaxAmount;
    }

    public void setDeductibleTaxAmount(BigDecimal deductibleTaxAmount) {
        this.deductibleTaxAmount = deductibleTaxAmount;
    }

    public String getArchiveStatus() {
        return archiveStatus;
    }

    public void setArchiveStatus(String archiveStatus) {
        this.archiveStatus = archiveStatus;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
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
