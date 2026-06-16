package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InvoiceQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String invoiceNo;
    private List<String> dateRange = new ArrayList<String>();

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public List<String> getDateRange() {
        return dateRange;
    }

    public void setDateRange(List<String> dateRange) {
        this.dateRange = dateRange;
    }
}
