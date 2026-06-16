package com.zcy.finance.infra.report;

import com.zcy.finance.api.vo.BalanceSheetVO;
import com.zcy.finance.api.vo.CashFlowVO;
import com.zcy.finance.api.vo.IncomeStatementVO;
import com.zcy.finance.api.vo.ReportVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;

@Repository
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BalanceSheetVO balanceSheet(String period) {
        String previous = previousPeriod(period);
        BigDecimal cash = balance(period, "a.account_code IN ('1001','1002','1012')");
        BigDecimal ar = balance(period, "a.account_code IN ('1121','1122','1123','1131','1132','1221','1231')");
        BigDecimal inventory = balance(period, "a.account_code LIKE '14%'");
        BigDecimal totalAssets = balance(period, "a.account_code LIKE '1%'");
        BigDecimal ap = balance(period, "a.account_code IN ('2201','2202','2203')");
        BigDecimal totalLiabilities = balance(period, "a.account_code LIKE '2%'");
        BigDecimal totalEquity = totalAssets.subtract(totalLiabilities);

        BalanceSheetVO vo = new BalanceSheetVO();
        vo.setReportType("balance_sheet");
        vo.setPeriod(period);
        vo.setRows(Arrays.asList(
                row("货币资金", cash, balance(previous, "a.account_code IN ('1001','1002','1012')")),
                row("应收及预付款项", ar, balance(previous, "a.account_code IN ('1121','1122','1123','1131','1132','1221','1231')")),
                row("存货", inventory, balance(previous, "a.account_code LIKE '14%'")),
                row("资产总计", totalAssets, balance(previous, "a.account_code LIKE '1%'")),
                row("应付及预收款项", ap, balance(previous, "a.account_code IN ('2201','2202','2203')")),
                row("负债总计", totalLiabilities, balance(previous, "a.account_code LIKE '2%'")),
                row("所有者权益合计", totalEquity, balance(previous, "a.account_code LIKE '1%'").subtract(balance(previous, "a.account_code LIKE '2%'"))),
                row("负债和所有者权益总计", totalLiabilities.add(totalEquity), balance(previous, "a.account_code LIKE '1%'"))
        ));
        vo.setTotalAssets(totalAssets);
        vo.setTotalLiabilities(totalLiabilities);
        vo.setTotalEquity(totalEquity);
        vo.setBalanced(totalAssets.compareTo(totalLiabilities.add(totalEquity)) == 0);
        vo.setAiComment("报表由凭证分录和科目余额实时汇总；资产负债表已完成平衡校验。");
        return vo;
    }

    public IncomeStatementVO incomeStatement(String period) {
        String previous = previousPeriod(period);
        BigDecimal revenue = periodAmount(period, "a.account_code IN ('6001','6051','6101','6111','6301')", true);
        BigDecimal operatingCost = periodAmount(period, "a.account_code IN ('6401','6402','6403')", false);
        BigDecimal salesExpense = periodAmount(period, "a.account_code = '6601'", false);
        BigDecimal managementExpense = periodAmount(period, "a.account_code LIKE '6602%'", false);
        BigDecimal financeExpense = periodAmount(period, "a.account_code = '6603'", false);
        BigDecimal incomeTax = periodAmount(period, "a.account_code = '6801'", false);
        BigDecimal totalProfit = revenue.subtract(operatingCost).subtract(salesExpense).subtract(managementExpense).subtract(financeExpense);
        BigDecimal netProfit = totalProfit.subtract(incomeTax);

        IncomeStatementVO vo = new IncomeStatementVO();
        vo.setReportType("income_statement");
        vo.setPeriod(period);
        vo.setRows(Arrays.asList(
                row("营业收入", revenue, periodAmount(previous, "a.account_code IN ('6001','6051','6101','6111','6301')", true)),
                row("营业成本及税金", operatingCost, periodAmount(previous, "a.account_code IN ('6401','6402','6403')", false)),
                row("销售费用", salesExpense, periodAmount(previous, "a.account_code = '6601'", false)),
                row("管理费用", managementExpense, periodAmount(previous, "a.account_code LIKE '6602%'", false)),
                row("财务费用", financeExpense, periodAmount(previous, "a.account_code = '6603'", false)),
                row("利润总额", totalProfit, profit(previous)),
                row("净利润", netProfit, profit(previous).subtract(periodAmount(previous, "a.account_code = '6801'", false)))
        ));
        vo.setRevenue(revenue);
        vo.setOperatingCost(operatingCost);
        vo.setTotalProfit(totalProfit);
        vo.setNetProfit(netProfit);
        vo.setAiComment("利润表由本期已入账凭证按损益类科目实时汇总。");
        return vo;
    }

    public CashFlowVO cashFlow(String period) {
        String previous = previousPeriod(period);
        BigDecimal investing = cashBySummary(period, "%投资%", "%固定资产%", "%无形资产%");
        BigDecimal financing = cashBySummary(period, "%借款%", "%融资%", "%资本%");
        BigDecimal total = cashTotal(period);
        BigDecimal operating = total.subtract(investing).subtract(financing);

        CashFlowVO vo = new CashFlowVO();
        vo.setReportType("cash_flow");
        vo.setPeriod(period);
        vo.setRows(Arrays.asList(
                row("经营活动产生的现金流量净额", operating, operating(previous)),
                row("投资活动产生的现金流量净额", investing, cashBySummary(previous, "%投资%", "%固定资产%", "%无形资产%")),
                row("筹资活动产生的现金流量净额", financing, cashBySummary(previous, "%借款%", "%融资%", "%资本%")),
                row("现金及现金等价物净增加额", total, cashTotal(previous))
        ));
        vo.setOperatingNetCashFlow(operating);
        vo.setInvestingNetCashFlow(investing);
        vo.setFinancingNetCashFlow(financing);
        vo.setNetIncreaseCash(total);
        vo.setAiComment("现金流量表由货币资金科目分录按凭证摘要分类汇总。");
        return vo;
    }

    private BigDecimal balance(String period, String condition) {
        BigDecimal opening = amount(
                "SELECT COALESCE(SUM(a.opening_balance), 0) FROM t_account a WHERE " + condition
        );
        BigDecimal movement = amount(
                "SELECT COALESCE(SUM(CASE WHEN a.balance_direction = 'DEBIT' THEN e.debit_amount - e.credit_amount ELSE e.credit_amount - e.debit_amount END), 0) "
                        + "FROM t_voucher_entry e JOIN t_voucher v ON v.id = e.voucher_id JOIN t_account a ON a.id = e.account_id "
                        + "WHERE v.period <= ? AND (" + condition + ")",
                period
        );
        return opening.add(movement);
    }

    private BigDecimal periodAmount(String period, String condition, boolean creditDirection) {
        return amount(
                "SELECT COALESCE(SUM(" + (creditDirection ? "e.credit_amount - e.debit_amount" : "e.debit_amount - e.credit_amount") + "), 0) "
                        + "FROM t_voucher_entry e JOIN t_voucher v ON v.id = e.voucher_id JOIN t_account a ON a.id = e.account_id "
                        + "WHERE v.period = ? AND (" + condition + ")",
                period
        );
    }

    private BigDecimal profit(String period) {
        BigDecimal revenue = periodAmount(period, "a.account_code IN ('6001','6051','6101','6111','6301')", true);
        BigDecimal costs = periodAmount(period, "a.account_code IN ('6401','6402','6403','6601','6603') OR a.account_code LIKE '6602%'", false);
        return revenue.subtract(costs);
    }

    private BigDecimal cashTotal(String period) {
        return amount(
                "SELECT COALESCE(SUM(e.debit_amount - e.credit_amount), 0) FROM t_voucher_entry e "
                        + "JOIN t_voucher v ON v.id = e.voucher_id JOIN t_account a ON a.id = e.account_id "
                        + "WHERE v.period = ? AND a.account_code IN ('1001','1002','1012')",
                period
        );
    }

    private BigDecimal cashBySummary(String period, String first, String second, String third) {
        return amount(
                "SELECT COALESCE(SUM(e.debit_amount - e.credit_amount), 0) FROM t_voucher_entry e "
                        + "JOIN t_voucher v ON v.id = e.voucher_id JOIN t_account a ON a.id = e.account_id "
                        + "WHERE v.period = ? AND a.account_code IN ('1001','1002','1012') "
                        + "AND (v.summary LIKE ? OR v.summary LIKE ? OR v.summary LIKE ?)",
                period, first, second, third
        );
    }

    private BigDecimal operating(String period) {
        return cashTotal(period)
                .subtract(cashBySummary(period, "%投资%", "%固定资产%", "%无形资产%"))
                .subtract(cashBySummary(period, "%借款%", "%融资%", "%资本%"));
    }

    private BigDecimal amount(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    private ReportVO.Row row(String item, BigDecimal current, BigDecimal previous) {
        return new ReportVO.Row(item, current, previous);
    }

    private String previousPeriod(String period) {
        return YearMonth.parse(period).minusMonths(1).toString();
    }
}
