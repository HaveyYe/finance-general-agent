package com.zcy.finance.infra.data;

import com.zcy.finance.api.vo.ArDashboardVO;
import com.zcy.finance.api.vo.InvoiceVO;
import com.zcy.finance.api.vo.ReportVO;
import com.zcy.finance.api.vo.ExpenseVO;
import com.zcy.finance.api.vo.BankTransactionVO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DemoFinanceData {

    private DemoFinanceData() {
    }

    public static ArDashboardVO arDashboard(String month) {
        ArDashboardVO vo = new ArDashboardVO();
        vo.setMonth(month);
        vo.setReceivableAmount(new BigDecimal("1286500.00"));
        vo.setCollectedAmount(new BigDecimal("846300.00"));
        vo.setOverdueAmount(new BigDecimal("218900.00"));
        vo.setCollectionRate(new BigDecimal("65.78"));
        vo.setAgingBuckets(Arrays.asList(
                new ArDashboardVO.AgingBucket("0-30天", new BigDecimal("641200.00")),
                new ArDashboardVO.AgingBucket("31-60天", new BigDecimal("246400.00")),
                new ArDashboardVO.AgingBucket("61-90天", new BigDecimal("180000.00")),
                new ArDashboardVO.AgingBucket("90天以上", new BigDecimal("218900.00"))
        ));
        vo.setAdvices(Arrays.asList(
                "90天以上逾期占比偏高，建议优先跟进信用账期较长客户",
                "本月回款率为65.78%，建议对账后生成催收清单"
        ));
        return vo;
    }

    public static List<InvoiceVO> invoices() {
        List<InvoiceVO> rows = new ArrayList<InvoiceVO>();
        rows.add(invoice("INV-202605-001", "2026-05-03", "杭州云启科技有限公司", "浙江智造设备有限公司", "86000.00", "11180.00", "已验真"));
        rows.add(invoice("INV-202605-002", "2026-05-12", "杭州云启科技有限公司", "上海数科服务有限公司", "42000.00", "2520.00", "已验真"));
        rows.add(invoice("INV-202605-003", "2026-05-20", "杭州云启科技有限公司", "深圳蓝海咨询有限公司", "12800.00", "768.00", "待复核"));
        return rows;
    }

    public static List<ReportVO.Row> balanceSheetRows() {
        return Arrays.asList(
                new ReportVO.Row("货币资金", new BigDecimal("935000.00"), new BigDecimal("812000.00")),
                new ReportVO.Row("应收账款", new BigDecimal("1286500.00"), new BigDecimal("1062000.00")),
                new ReportVO.Row("存货", new BigDecimal("420000.00"), new BigDecimal("398000.00")),
                new ReportVO.Row("资产总计", new BigDecimal("2641500.00"), new BigDecimal("2272000.00")),
                new ReportVO.Row("应付账款", new BigDecimal("612000.00"), new BigDecimal("580000.00")),
                new ReportVO.Row("负债总计", new BigDecimal("612000.00"), new BigDecimal("580000.00")),
                new ReportVO.Row("所有者权益合计", new BigDecimal("2029500.00"), new BigDecimal("1692000.00")),
                new ReportVO.Row("负债和所有者权益总计", new BigDecimal("2641500.00"), new BigDecimal("2272000.00"))
        );
    }

    public static List<ReportVO.Row> incomeStatementRows() {
        return Arrays.asList(
                new ReportVO.Row("营业收入", new BigDecimal("2450000.00"), new BigDecimal("2180000.00")),
                new ReportVO.Row("营业成本", new BigDecimal("1480000.00"), new BigDecimal("1375000.00")),
                new ReportVO.Row("销售费用", new BigDecimal("286000.00"), new BigDecimal("255000.00")),
                new ReportVO.Row("管理费用", new BigDecimal("198000.00"), new BigDecimal("186000.00")),
                new ReportVO.Row("营业利润", new BigDecimal("486000.00"), new BigDecimal("364000.00")),
                new ReportVO.Row("利润总额", new BigDecimal("478000.00"), new BigDecimal("360000.00")),
                new ReportVO.Row("净利润", new BigDecimal("358500.00"), new BigDecimal("270000.00"))
        );
    }

    public static List<ReportVO.Row> cashFlowStatementRows() {
        return Arrays.asList(
                new ReportVO.Row("经营活动现金流入小计", new BigDecimal("2320000.00"), new BigDecimal("2050000.00")),
                new ReportVO.Row("经营活动现金流出小计", new BigDecimal("1915000.00"), new BigDecimal("1780000.00")),
                new ReportVO.Row("经营活动产生的现金流量净额", new BigDecimal("405000.00"), new BigDecimal("270000.00")),
                new ReportVO.Row("投资活动产生的现金流量净额", new BigDecimal("-186000.00"), new BigDecimal("-142000.00")),
                new ReportVO.Row("筹资活动产生的现金流量净额", new BigDecimal("-96000.00"), new BigDecimal("45000.00")),
                new ReportVO.Row("现金及现金等价物净增加额", new BigDecimal("123000.00"), new BigDecimal("173000.00"))
        );
    }

    public static List<ExpenseVO> expenses() {
        List<ExpenseVO> rows = new ArrayList<ExpenseVO>();
        rows.add(expense("EXP-202605-001", "E1001", "张三", "2026-05-09", "3200.00", "APPROVED", "金额与差旅制度匹配"));
        rows.add(expense("EXP-202605-002", "E1002", "李四", "2026-05-16", "8600.00", "PENDING", "餐饮金额偏高，建议复核招待对象"));
        rows.add(expense("EXP-202605-003", "E1003", "王五", "2026-05-22", "1299.00", "REJECTED", "缺少发票附件"));
        return rows;
    }

    public static List<BankTransactionVO> bankTransactions() {
        List<BankTransactionVO> rows = new ArrayList<BankTransactionVO>();
        rows.add(bankTransaction("BT-202605-001", "6222-0001", "2026-05-03", "浙江智造设备有限公司", "客户回款", "0.00", "120000.00", "1055000.00", "MATCHED", "V-20260503-0001", "已自动匹配销售回款凭证"));
        rows.add(bankTransaction("BT-202605-002", "6222-0001", "2026-05-09", "杭州差旅服务有限公司", "差旅报销付款", "3200.00", "0.00", "1051800.00", "MATCHED", "V-20260509-0003", "已匹配费用报销单"));
        rows.add(bankTransaction("BT-202605-003", "6222-0001", "2026-05-16", "李四", "员工报销付款", "8600.00", "0.00", "1043200.00", "UNMATCHED", "", "报销单仍在审批中，暂未生成凭证"));
        rows.add(bankTransaction("BT-202605-004", "6222-0001", "2026-05-22", "上海数科服务有限公司", "软件服务费", "42000.00", "0.00", "1001200.00", "UNMATCHED", "", "供应商发票已验真但凭证未入账"));
        rows.add(bankTransaction("BT-202605-005", "6222-0001", "2026-05-29", "深圳蓝海咨询有限公司", "咨询费付款", "12800.00", "0.00", "988400.00", "RISK", "", "发票待复核，建议暂缓自动勾对"));
        return rows;
    }

    private static InvoiceVO invoice(String invoiceNo, String invoiceDate, String buyerName, String sellerName, String amount, String taxAmount, String verifyStatus) {
        InvoiceVO vo = new InvoiceVO();
        vo.setInvoiceNo(invoiceNo);
        vo.setInvoiceDate(invoiceDate);
        vo.setBuyerName(buyerName);
        vo.setSellerName(sellerName);
        vo.setAmount(new BigDecimal(amount));
        vo.setTaxAmount(new BigDecimal(taxAmount));
        vo.setVerifyStatus(verifyStatus);
        return vo;
    }

    private static ExpenseVO expense(String expenseNo, String employeeId, String employeeName, String expenseDate, String amount, String status, String riskHint) {
        ExpenseVO vo = new ExpenseVO();
        vo.setExpenseNo(expenseNo);
        vo.setEmployeeId(employeeId);
        vo.setEmployeeName(employeeName);
        vo.setExpenseDate(expenseDate);
        vo.setAmount(new BigDecimal(amount));
        vo.setStatus(status);
        vo.setRiskHint(riskHint);
        return vo;
    }

    private static BankTransactionVO bankTransaction(String transactionNo, String accountNo, String transactionDate, String counterparty,
                                                     String summary, String debitAmount, String creditAmount, String balance,
                                                     String status, String matchedVoucherNo, String riskHint) {
        BankTransactionVO vo = new BankTransactionVO();
        vo.setTransactionNo(transactionNo);
        vo.setAccountNo(accountNo);
        vo.setTransactionDate(transactionDate);
        vo.setCounterparty(counterparty);
        vo.setSummary(summary);
        vo.setDebitAmount(new BigDecimal(debitAmount));
        vo.setCreditAmount(new BigDecimal(creditAmount));
        vo.setBalance(new BigDecimal(balance));
        vo.setStatus(status);
        vo.setMatchedVoucherNo(matchedVoucherNo);
        vo.setRiskHint(riskHint);
        return vo;
    }
}
