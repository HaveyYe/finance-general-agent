package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TaxReturnVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String returnNo;
    private String taxType;
    private String period;
    private BigDecimal salesAmount;
    private BigDecimal outputTax;
    private BigDecimal inputTax;
    private BigDecimal taxPayable;
    private String status;
    private List<Row> rows = new ArrayList<Row>();
    private List<String> riskHints = new ArrayList<String>();

    public String getReturnNo() {
        return returnNo;
    }

    public void setReturnNo(String returnNo) {
        this.returnNo = returnNo;
    }

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

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }

    public BigDecimal getOutputTax() {
        return outputTax;
    }

    public void setOutputTax(BigDecimal outputTax) {
        this.outputTax = outputTax;
    }

    public BigDecimal getInputTax() {
        return inputTax;
    }

    public void setInputTax(BigDecimal inputTax) {
        this.inputTax = inputTax;
    }

    public BigDecimal getTaxPayable() {
        return taxPayable;
    }

    public void setTaxPayable(BigDecimal taxPayable) {
        this.taxPayable = taxPayable;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    public List<String> getRiskHints() {
        return riskHints;
    }

    public void setRiskHints(List<String> riskHints) {
        this.riskHints = riskHints;
    }

    public static class Row implements Serializable {
        private static final long serialVersionUID = 1L;

        private String item;
        private BigDecimal amount;
        private String remark;

        public Row() {
        }

        public Row(String item, BigDecimal amount, String remark) {
            this.item = item;
            this.amount = amount;
            this.remark = remark;
        }

        public String getItem() {
            return item;
        }

        public void setItem(String item) {
            this.item = item;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
