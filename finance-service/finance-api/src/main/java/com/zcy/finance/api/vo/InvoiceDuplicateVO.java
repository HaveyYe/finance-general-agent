package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDuplicateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String duplicateNo;
    private String invoiceCode;
    private String invoiceNo;
    private String matchedInvoiceNo;
    private String matchedSource;
    private String duplicateType;
    private String riskLevel;
    private BigDecimal similarity = BigDecimal.ZERO;
    private String status;
    private List<String> evidence = new ArrayList<String>();
    private List<String> suggestions = new ArrayList<String>();

    public String getDuplicateNo() { return duplicateNo; }
    public void setDuplicateNo(String duplicateNo) { this.duplicateNo = duplicateNo; }
    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getMatchedInvoiceNo() { return matchedInvoiceNo; }
    public void setMatchedInvoiceNo(String matchedInvoiceNo) { this.matchedInvoiceNo = matchedInvoiceNo; }
    public String getMatchedSource() { return matchedSource; }
    public void setMatchedSource(String matchedSource) { this.matchedSource = matchedSource; }
    public String getDuplicateType() { return duplicateType; }
    public void setDuplicateType(String duplicateType) { this.duplicateType = duplicateType; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public BigDecimal getSimilarity() { return similarity; }
    public void setSimilarity(BigDecimal similarity) { this.similarity = similarity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}
