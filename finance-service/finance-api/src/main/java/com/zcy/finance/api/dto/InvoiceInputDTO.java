package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class InvoiceInputDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String invoiceCode;
    private String invoiceNo;
    private String invoiceDate;
    private String invoiceType;
    private String buyerName;
    private String sellerName;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private String source;
    private String fileHash;
    private boolean autoVerify;

    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; }
    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String invoiceType) { this.invoiceType = invoiceType; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public boolean isAutoVerify() { return autoVerify; }
    public void setAutoVerify(boolean autoVerify) { this.autoVerify = autoVerify; }
}
