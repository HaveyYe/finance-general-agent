package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ArDashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String month;
    private BigDecimal receivableAmount;
    private BigDecimal collectedAmount;
    private BigDecimal overdueAmount;
    private BigDecimal collectionRate;
    private List<AgingBucket> agingBuckets = new ArrayList<AgingBucket>();
    private List<String> advices = new ArrayList<String>();

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getReceivableAmount() {
        return receivableAmount;
    }

    public void setReceivableAmount(BigDecimal receivableAmount) {
        this.receivableAmount = receivableAmount;
    }

    public BigDecimal getCollectedAmount() {
        return collectedAmount;
    }

    public void setCollectedAmount(BigDecimal collectedAmount) {
        this.collectedAmount = collectedAmount;
    }

    public BigDecimal getOverdueAmount() {
        return overdueAmount;
    }

    public void setOverdueAmount(BigDecimal overdueAmount) {
        this.overdueAmount = overdueAmount;
    }

    public BigDecimal getCollectionRate() {
        return collectionRate;
    }

    public void setCollectionRate(BigDecimal collectionRate) {
        this.collectionRate = collectionRate;
    }

    public List<AgingBucket> getAgingBuckets() {
        return agingBuckets;
    }

    public void setAgingBuckets(List<AgingBucket> agingBuckets) {
        this.agingBuckets = agingBuckets;
    }

    public List<String> getAdvices() {
        return advices;
    }

    public void setAdvices(List<String> advices) {
        this.advices = advices;
    }

    public static class AgingBucket implements Serializable {
        private static final long serialVersionUID = 1L;

        private String bucket;
        private BigDecimal amount;

        public AgingBucket() {
        }

        public AgingBucket(String bucket, BigDecimal amount) {
            this.bucket = bucket;
            this.amount = amount;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
