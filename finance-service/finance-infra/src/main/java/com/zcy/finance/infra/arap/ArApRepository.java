package com.zcy.finance.infra.arap;

import com.zcy.finance.api.vo.AgingVO;
import com.zcy.finance.api.vo.ArDashboardVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class ArApRepository {

    private final JdbcTemplate jdbcTemplate;

    public ArApRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AgingVO> queryAging(String type, String baseDate) {
        LocalDate base = LocalDate.parse(baseDate);
        List<ArApRecord> records = records(type);
        List<AgingVO> rows = new ArrayList<AgingVO>();
        for (ArApRecord record : records) {
            long days = ChronoUnit.DAYS.between(record.dueDate, base);
            AgingVO row = new AgingVO();
            row.setPartnerId(record.partnerId);
            row.setPartnerName(record.partnerName);
            row.setType(record.type);
            row.setBaseDate(baseDate);
            row.setTotalAmount(record.amount);
            row.setWithin30Days(days <= 30 ? record.amount : BigDecimal.ZERO);
            row.setDays31To60(days > 30 && days <= 60 ? record.amount : BigDecimal.ZERO);
            row.setDays61To90(days > 60 && days <= 90 ? record.amount : BigDecimal.ZERO);
            row.setOver90Days(days > 90 ? record.amount : BigDecimal.ZERO);
            row.setOverdueAmount(days > 0 ? record.amount : BigDecimal.ZERO);
            row.setOldestOverdueDays((int) Math.max(days, 0));
            row.setRiskLevel(days > 90 ? "HIGH" : days > 30 ? "MEDIUM" : "LOW");
            row.setSuggestion(days > 90
                    ? "已逾期超过90天，建议升级催收或付款风险复核。"
                    : days > 0 ? "款项已逾期，建议立即跟进并确认处理计划。" : "款项未逾期，按账期持续跟踪。");
            rows.add(row);
        }
        return rows;
    }

    public ArDashboardVO dashboard(String month) {
        LocalDate baseDate = YearMonth.parse(month).atEndOfMonth();
        List<AgingVO> aging = queryAging("AR", baseDate.toString());
        BigDecimal receivable = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;
        BigDecimal within30 = BigDecimal.ZERO;
        BigDecimal days31To60 = BigDecimal.ZERO;
        BigDecimal days61To90 = BigDecimal.ZERO;
        BigDecimal over90 = BigDecimal.ZERO;
        for (AgingVO row : aging) {
            receivable = receivable.add(row.getTotalAmount());
            overdue = overdue.add(row.getOverdueAmount());
            within30 = within30.add(row.getWithin30Days());
            days31To60 = days31To60.add(row.getDays31To60());
            days61To90 = days61To90.add(row.getDays61To90());
            over90 = over90.add(row.getOver90Days());
        }
        BigDecimal collected = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(e.debit_amount), 0) FROM t_voucher_entry e "
                        + "JOIN t_voucher v ON v.id = e.voucher_id "
                        + "JOIN t_account a ON a.id = e.account_id "
                        + "WHERE v.period = ? AND v.summary LIKE ? AND a.account_code IN ('1001','1002','1012')",
                BigDecimal.class, month, "%销售回款%"
        );
        collected = collected == null ? BigDecimal.ZERO : collected;

        ArDashboardVO dashboard = new ArDashboardVO();
        dashboard.setMonth(month);
        dashboard.setReceivableAmount(receivable);
        dashboard.setCollectedAmount(collected);
        dashboard.setOverdueAmount(overdue);
        BigDecimal denominator = receivable.add(collected);
        dashboard.setCollectionRate(denominator.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO : collected.multiply(new BigDecimal("100")).divide(denominator, 2, RoundingMode.HALF_UP));
        dashboard.setAgingBuckets(Arrays.asList(
                new ArDashboardVO.AgingBucket("0-30天", within30),
                new ArDashboardVO.AgingBucket("31-60天", days31To60),
                new ArDashboardVO.AgingBucket("61-90天", days61To90),
                new ArDashboardVO.AgingBucket("90天以上", over90)
        ));
        dashboard.setAdvices(Arrays.asList(
                over90.compareTo(BigDecimal.ZERO) > 0 ? "存在90天以上应收，建议升级催收并复核客户授信。" : "当前无90天以上应收。",
                "本月回款率为" + dashboard.getCollectionRate() + "%，建议结合账龄明细制定催收计划。"
        ));
        return dashboard;
    }

    private List<ArApRecord> records(String type) {
        return jdbcTemplate.query(
                "SELECT record_type, partner_id, partner_name, amount, due_date, status FROM t_ar_ap_record WHERE record_type = ? ORDER BY amount DESC",
                (rs, rowNum) -> new ArApRecord(
                        rs.getString("record_type"),
                        rs.getString("partner_id"),
                        rs.getString("partner_name"),
                        rs.getBigDecimal("amount"),
                        rs.getDate("due_date").toLocalDate(),
                        rs.getString("status")
                ),
                type
        );
    }

    private static class ArApRecord {
        private final String type;
        private final String partnerId;
        private final String partnerName;
        private final BigDecimal amount;
        private final LocalDate dueDate;
        @SuppressWarnings("unused")
        private final String status;

        private ArApRecord(String type, String partnerId, String partnerName, BigDecimal amount, LocalDate dueDate, String status) {
            this.type = type;
            this.partnerId = partnerId;
            this.partnerName = partnerName;
            this.amount = amount;
            this.dueDate = dueDate;
            this.status = status;
        }
    }
}
