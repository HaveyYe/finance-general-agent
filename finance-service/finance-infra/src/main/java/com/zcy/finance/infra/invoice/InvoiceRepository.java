package com.zcy.finance.infra.invoice;

import com.zcy.finance.api.vo.InvoiceVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class InvoiceRepository {

    private final JdbcTemplate jdbcTemplate;

    public InvoiceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(InvoiceVO invoice) {
        jdbcTemplate.update(
                "INSERT INTO t_invoice (invoice_code, invoice_no, invoice_date, invoice_type, direction, partner_id, buyer_name, seller_name, amount, tax_amount, status, input_status, source, file_hash) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                invoice.getInvoiceCode(), invoice.getInvoiceNo(), invoice.getInvoiceDate(), invoice.getInvoiceType(), "INPUT",
                defaultString(invoice.getSellerName(), "UNKNOWN"), invoice.getBuyerName(), invoice.getSellerName(),
                invoice.getAmount(), invoice.getTaxAmount(), toDbStatus(invoice.getVerifyStatus()), invoice.getInputStatus(),
                invoice.getSource(), invoice.getFileHash()
        );
    }

    public InvoiceVO findByNo(String invoiceNo) {
        List<InvoiceVO> rows = jdbcTemplate.query(querySql() + " WHERE i.invoice_no = ?", this::map, invoiceNo);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<InvoiceVO> findAll() {
        return jdbcTemplate.query(querySql() + " ORDER BY i.invoice_date DESC, i.id DESC", this::map);
    }

    private String querySql() {
        return "SELECT i.*, s.supplier_name, c.customer_name FROM t_invoice i "
                + "LEFT JOIN t_supplier s ON s.supplier_code = i.partner_id "
                + "LEFT JOIN t_customer c ON c.customer_code = i.partner_id";
    }

    private InvoiceVO map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        InvoiceVO invoice = new InvoiceVO();
        invoice.setInvoiceCode(rs.getString("invoice_code"));
        invoice.setInvoiceNo(rs.getString("invoice_no"));
        invoice.setInvoiceDate(rs.getDate("invoice_date").toString());
        invoice.setInvoiceType(rs.getString("invoice_type"));
        String direction = rs.getString("direction");
        invoice.setBuyerName(defaultString(rs.getString("buyer_name"), "INPUT".equals(direction) ? "杭州云启科技有限公司" : rs.getString("customer_name")));
        invoice.setSellerName(defaultString(rs.getString("seller_name"), "INPUT".equals(direction) ? rs.getString("supplier_name") : "杭州云启科技有限公司"));
        invoice.setAmount(rs.getBigDecimal("amount"));
        invoice.setTaxAmount(rs.getBigDecimal("tax_amount"));
        invoice.setTotalAmount(zero(invoice.getAmount()).add(zero(invoice.getTaxAmount())));
        invoice.setVerifyStatus(fromDbStatus(rs.getString("status")));
        invoice.setInputStatus(defaultString(rs.getString("input_status"), "RECORDED"));
        invoice.setSource(defaultString(rs.getString("source"), "DEMO_LEDGER"));
        invoice.setFileHash(defaultString(rs.getString("file_hash"), "HASH-" + invoice.getInvoiceNo()));
        invoice.setRiskLevel("待复核".equals(invoice.getVerifyStatus()) ? "MEDIUM" : "LOW");
        return invoice;
    }

    private String toDbStatus(String value) {
        return "待复核".equals(value) || "待自动验真".equals(value) ? "NEED_REVIEW" : "VERIFIED";
    }

    private String fromDbStatus(String value) {
        return "NEED_REVIEW".equals(value) ? "待复核" : "已验真";
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }
}
