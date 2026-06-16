package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;

public class InvoiceDupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String invoiceCode;
    private String invoiceNo;
    private String matchedInvoiceNo;
    private String duplicateType;
    private BigDecimal similarity = BigDecimal.ZERO;
    private String source;
    private String riskLevel;
    private String suggestion;

    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getMatchedInvoiceNo() { return matchedInvoiceNo; }
    public void setMatchedInvoiceNo(String matchedInvoiceNo) { this.matchedInvoiceNo = matchedInvoiceNo; }
    public String getDuplicateType() { return duplicateType; }
    public void setDuplicateType(String duplicateType) { this.duplicateType = duplicateType; }
    public BigDecimal getSimilarity() { return similarity; }
    public void setSimilarity(BigDecimal similarity) { this.similarity = similarity; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
}
