package com.zcy.finance.infra.expense;

import com.zcy.finance.api.vo.ExpenseVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigDecimal;
import java.time.YearMonth;

@Repository
public class ExpenseRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextExpenseNo(String period) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_expense WHERE expense_date >= ? AND expense_date < ?",
                Integer.class, period + "-01", nextMonth(period) + "-01"
        );
        return "EXP-" + period.replace("-", "") + "-" + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    public void insert(ExpenseVO expense) {
        jdbcTemplate.update(
                "INSERT INTO t_expense (expense_no, employee_id, employee_name, department, project_code, expense_type, description, amount, expense_date, approval_status, invoice_count, attachment_count, risk_hint) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                expense.getExpenseNo(), expense.getEmployeeId(), expense.getEmployeeName(), expense.getDepartment(),
                expense.getProjectCode(), expense.getExpenseType(), expense.getDescription(), expense.getAmount(),
                expense.getExpenseDate(), expense.getStatus(), expense.getInvoiceCount(), expense.getAttachmentCount(), expense.getRiskHint()
        );
    }

    public List<ExpenseVO> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM t_expense ORDER BY expense_date DESC, id DESC",
                (rs, rowNum) -> {
                    ExpenseVO expense = new ExpenseVO();
                    expense.setExpenseNo(rs.getString("expense_no"));
                    expense.setEmployeeId(rs.getString("employee_id"));
                    expense.setEmployeeName(rs.getString("employee_name"));
                    expense.setDepartment(rs.getString("department"));
                    expense.setProjectCode(rs.getString("project_code"));
                    expense.setExpenseType(rs.getString("expense_type"));
                    expense.setDescription(rs.getString("description"));
                    expense.setAmount(rs.getBigDecimal("amount"));
                    expense.setExpenseDate(rs.getDate("expense_date").toString());
                    expense.setStatus(rs.getString("approval_status"));
                    expense.setInvoiceCount(rs.getInt("invoice_count"));
                    expense.setAttachmentCount(rs.getInt("attachment_count"));
                    expense.setRiskHint(rs.getString("risk_hint"));
                    return expense;
                }
        );
    }

    public boolean exists(String expenseNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_expense WHERE expense_no = ?",
                Integer.class,
                expenseNo
        );
        return count != null && count > 0;
    }

    public void updateApprovalStatus(String expenseNo, String approvalStatus, String riskHint) {
        jdbcTemplate.update(
                "UPDATE t_expense SET approval_status = ?, risk_hint = ? WHERE expense_no = ?",
                approvalStatus, riskHint, expenseNo
        );
    }

    public BigDecimal[] budgetSummary(String department, String period) {
        YearMonth yearMonth = YearMonth.parse(period);
        BigDecimal[] budget = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(budget_amount), 0), COALESCE(SUM(used_amount), 0) FROM t_budget "
                        + "WHERE department = ? AND budget_year = ? AND budget_month = ?",
                (rs, rowNum) -> new BigDecimal[]{rs.getBigDecimal(1), rs.getBigDecimal(2)},
                department, yearMonth.getYear(), yearMonth.getMonthValue()
        );
        BigDecimal occupied = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM t_expense WHERE department = ? "
                        + "AND expense_date >= ? AND expense_date < ? "
                        + "AND approval_status IN ('PENDING', 'NEED_REVIEW', 'APPROVED_ROUTE_READY')",
                BigDecimal.class,
                department, period + "-01", yearMonth.plusMonths(1) + "-01"
        );
        return new BigDecimal[]{
                budget == null || budget[0] == null ? BigDecimal.ZERO : budget[0],
                budget == null || budget[1] == null ? BigDecimal.ZERO : budget[1],
                occupied == null ? BigDecimal.ZERO : occupied
        };
    }

    private String nextMonth(String period) {
        int year = Integer.parseInt(period.substring(0, 4));
        int month = Integer.parseInt(period.substring(5, 7));
        return month == 12 ? (year + 1) + "-01" : String.format("%04d-%02d", year, month + 1);
    }
}
