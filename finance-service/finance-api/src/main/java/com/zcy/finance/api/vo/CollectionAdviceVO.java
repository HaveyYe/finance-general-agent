package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;

public class CollectionAdviceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String adviceNo;
    private String customerId;
    private String customerName;
    private BigDecimal amount = BigDecimal.ZERO;
    private int overdueDays;
    private String priority;
    private String channel;
    private String owner;
    private String dueDate;
    private String wording;

    public String getAdviceNo() { return adviceNo; }
    public void setAdviceNo(String adviceNo) { this.adviceNo = adviceNo; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getOverdueDays() { return overdueDays; }
    public void setOverdueDays(int overdueDays) { this.overdueDays = overdueDays; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getWording() { return wording; }
    public void setWording(String wording) { this.wording = wording; }
}
