package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InvoiceCheckVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String invoiceCode;
    private String invoiceNo;
    private boolean authentic;
    private boolean amountMatched;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private String sellerName;
    private String checkStatus;
    private String checkedAt;
    private String riskLevel;
    private List<String> hints = new ArrayList<String>();

    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public boolean isAuthentic() { return authentic; }
    public void setAuthentic(boolean authentic) { this.authentic = authentic; }
    public boolean isAmountMatched() { return amountMatched; }
    public void setAmountMatched(boolean amountMatched) { this.amountMatched = amountMatched; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getCheckStatus() { return checkStatus; }
    public void setCheckStatus(String checkStatus) { this.checkStatus = checkStatus; }
    public String getCheckedAt() { return checkedAt; }
    public void setCheckedAt(String checkedAt) { this.checkedAt = checkedAt; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<String> getHints() { return hints; }
    public void setHints(List<String> hints) { this.hints = hints; }
}
