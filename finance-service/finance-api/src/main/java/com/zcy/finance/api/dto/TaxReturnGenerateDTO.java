package com.zcy.finance.api.dto;

import java.io.Serializable;

public class TaxReturnGenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taxType;
    private String period;

    public String getTaxType() {
        return taxType;
    }

    public void setTaxType(String taxType) {
        this.taxType = taxType;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
