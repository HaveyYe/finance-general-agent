package com.zcy.finance.infra.bank;

import com.zcy.finance.api.vo.BankTransactionVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class BankTransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public BankTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BankTransactionVO> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM t_bank_transaction ORDER BY transaction_date DESC, id DESC",
                this::map
        );
    }

    public BigDecimal latestBalance(String accountNo) {
        List<BigDecimal> rows = jdbcTemplate.query(
                "SELECT balance FROM t_bank_transaction WHERE account_no = ? ORDER BY transaction_date DESC, id DESC LIMIT 1",
                (rs, rowNum) -> rs.getBigDecimal("balance"),
                accountNo
        );
        return rows.isEmpty() || rows.get(0) == null ? BigDecimal.ZERO : rows.get(0);
    }

    private BankTransactionVO map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        BankTransactionVO vo = new BankTransactionVO();
        vo.setTransactionNo(rs.getString("transaction_no"));
        vo.setAccountNo(rs.getString("account_no"));
        vo.setTransactionDate(rs.getDate("transaction_date").toString());
        vo.setCounterparty(rs.getString("counterparty"));
        vo.setSummary(rs.getString("summary"));
        vo.setDebitAmount(rs.getBigDecimal("debit_amount"));
        vo.setCreditAmount(rs.getBigDecimal("credit_amount"));
        vo.setBalance(rs.getBigDecimal("balance"));
        vo.setStatus(rs.getString("status"));
        vo.setMatchedVoucherNo(rs.getString("matched_voucher_no"));
        vo.setRiskHint(rs.getString("risk_hint"));
        return vo;
    }
}
