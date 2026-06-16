package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BankTransactionQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountNo;
    private String status;
    private List<String> dateRange = new ArrayList<String>();

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getDateRange() {
        return dateRange;
    }

    public void setDateRange(List<String> dateRange) {
        this.dateRange = dateRange;
    }
}
