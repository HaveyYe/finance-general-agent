package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportType;
    private String period;
    private List<Row> rows = new ArrayList<Row>();
    private String aiComment;

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    public String getAiComment() {
        return aiComment;
    }

    public void setAiComment(String aiComment) {
        this.aiComment = aiComment;
    }

    public static class Row implements Serializable {
        private static final long serialVersionUID = 1L;

        private String item;
        private BigDecimal currentAmount;
        private BigDecimal previousAmount;

        public Row() {
        }

        public Row(String item, BigDecimal currentAmount, BigDecimal previousAmount) {
            this.item = item;
            this.currentAmount = currentAmount;
            this.previousAmount = previousAmount;
        }

        public String getItem() {
            return item;
        }

        public void setItem(String item) {
            this.item = item;
        }

        public BigDecimal getCurrentAmount() {
            return currentAmount;
        }

        public void setCurrentAmount(BigDecimal currentAmount) {
            this.currentAmount = currentAmount;
        }

        public BigDecimal getPreviousAmount() {
            return previousAmount;
        }

        public void setPreviousAmount(BigDecimal previousAmount) {
            this.previousAmount = previousAmount;
        }
    }
}
