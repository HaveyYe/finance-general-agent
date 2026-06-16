package com.zcy.finance.api.dto;

import java.io.Serializable;

public class AgingQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String baseDate;
    private String partnerId;
    private String partnerName;
    private Integer pageNo = 1;
    private Integer pageSize = 20;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBaseDate() { return baseDate; }
    public void setBaseDate(String baseDate) { this.baseDate = baseDate; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
