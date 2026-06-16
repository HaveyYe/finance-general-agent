package com.zcy.finance.api.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total;
    private int pageNo;
    private int pageSize;
    private List<T> rows = new ArrayList<T>();

    public static <T> PageResult<T> of(List<T> rows, long total, int pageNo, int pageSize) {
        PageResult<T> result = new PageResult<T>();
        result.setRows(rows);
        result.setTotal(total);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        return result;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }
}
