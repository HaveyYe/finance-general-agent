package com.zcy.finance.infra.voucher;

import com.zcy.finance.api.vo.VoucherVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class VoucherRepository {

    private final JdbcTemplate jdbcTemplate;

    public VoucherRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextVoucherNo(String period) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_voucher WHERE period = ?", Integer.class, period);
        return "V-" + period.replace("-", "") + "-" + String.format("%04d", (count == null ? 0 : count) + 1);
    }

    @Transactional
    public void insert(VoucherVO voucher) {
        jdbcTemplate.update(
                "INSERT INTO t_voucher (voucher_no, voucher_date, period, summary, status, preparer, reviewer, source_document_no) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                voucher.getVoucherNo(), voucher.getVoucherDate(), voucher.getPeriod(), voucher.getSummary(), voucher.getStatus(),
                voucher.getPreparer(), voucher.getReviewer(), voucher.getSourceDocumentNo()
        );
        Long voucherId = jdbcTemplate.queryForObject("SELECT id FROM t_voucher WHERE voucher_no = ?", Long.class, voucher.getVoucherNo());
        for (VoucherVO.VoucherEntry entry : voucher.getEntries()) {
            Long accountId = jdbcTemplate.queryForObject("SELECT id FROM t_account WHERE account_code = ?", Long.class, entry.getAccountCode());
            jdbcTemplate.update(
                    "INSERT INTO t_voucher_entry (voucher_id, account_id, debit_amount, credit_amount, remark) VALUES (?, ?, ?, ?, ?)",
                    voucherId, accountId, zero(entry.getDebitAmount()), zero(entry.getCreditAmount()), entry.getRemark()
            );
        }
    }

    public VoucherVO findByNo(String voucherNo) {
        List<VoucherVO> rows = jdbcTemplate.query(
                "SELECT * FROM t_voucher WHERE voucher_no = ?",
                (rs, rowNum) -> mapVoucher(rs.getLong("id"), rs.getString("voucher_no"), rs.getDate("voucher_date").toString(),
                        rs.getString("period"), rs.getString("summary"), rs.getString("status"), rs.getString("preparer"),
                        rs.getString("reviewer"), rs.getString("source_document_no")),
                voucherNo
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<VoucherVO> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM t_voucher ORDER BY voucher_date DESC, id DESC",
                (rs, rowNum) -> mapVoucher(rs.getLong("id"), rs.getString("voucher_no"), rs.getDate("voucher_date").toString(),
                        rs.getString("period"), rs.getString("summary"), rs.getString("status"), rs.getString("preparer"),
                        rs.getString("reviewer"), rs.getString("source_document_no"))
        );
    }

    public boolean audit(String voucherNo, String auditor) {
        return jdbcTemplate.update("UPDATE t_voucher SET status = 'AUDIT_PASSED', reviewer = ? WHERE voucher_no = ?", auditor, voucherNo) > 0;
    }

    private VoucherVO mapVoucher(long voucherId, String voucherNo, String voucherDate, String period, String summary,
                                 String status, String preparer, String reviewer, String sourceDocumentNo) {
        VoucherVO voucher = new VoucherVO();
        voucher.setVoucherNo(voucherNo);
        voucher.setVoucherDate(voucherDate);
        voucher.setPeriod(period);
        voucher.setSummary(summary);
        voucher.setStatus(status);
        voucher.setPreparer(preparer);
        voucher.setReviewer(reviewer);
        voucher.setSourceDocumentNo(sourceDocumentNo);
        List<VoucherVO.VoucherEntry> entries = jdbcTemplate.query(
                "SELECT a.account_code, a.account_name, e.debit_amount, e.credit_amount, e.remark FROM t_voucher_entry e JOIN t_account a ON a.id = e.account_id WHERE e.voucher_id = ? ORDER BY e.id",
                (rs, rowNum) -> new VoucherVO.VoucherEntry(
                        rs.getString("account_code"),
                        rs.getString("account_name"),
                        rs.getBigDecimal("debit_amount").compareTo(BigDecimal.ZERO) > 0 ? "DEBIT" : "CREDIT",
                        rs.getBigDecimal("debit_amount"),
                        rs.getBigDecimal("credit_amount"),
                        rs.getString("remark")
                ),
                voucherId
        );
        voucher.setEntries(entries);
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (VoucherVO.VoucherEntry entry : entries) {
            debitTotal = debitTotal.add(zero(entry.getDebitAmount()));
            creditTotal = creditTotal.add(zero(entry.getCreditAmount()));
        }
        voucher.setDebitTotal(debitTotal);
        voucher.setCreditTotal(creditTotal);
        voucher.setAuditMessages(new ArrayList<String>(Arrays.asList(
                "AUDIT_PASSED".equals(status) ? "借贷平衡，凭证已通过审核。" : "凭证存在待复核事项，请查看自动审核结果。"
        )));
        return voucher;
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
