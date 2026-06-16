package com.zcy.finance.api.dto;

import java.io.Serializable;

public class BankReconcileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountNo;
    private String period;

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
