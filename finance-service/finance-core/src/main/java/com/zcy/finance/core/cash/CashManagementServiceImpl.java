package com.zcy.finance.core.cash;

import com.zcy.finance.api.CashManagementService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.CashFlowForecastDTO;
import com.zcy.finance.api.vo.ArDashboardVO;
import com.zcy.finance.api.vo.BankTransactionVO;
import com.zcy.finance.api.vo.CashFlowForecastVO;
import com.zcy.finance.infra.arap.ArApRepository;
import com.zcy.finance.infra.bank.BankTransactionRepository;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DubboService(interfaceClass = CashManagementService.class)
public class CashManagementServiceImpl implements CashManagementService {

    private static final BigDecimal SAFETY_LEVEL = new BigDecimal("650000.00");
    private final BankTransactionRepository bankRepository;
    private final ArApRepository arApRepository;

    public CashManagementServiceImpl(BankTransactionRepository bankRepository, ArApRepository arApRepository) {
        this.bankRepository = bankRepository;
        this.arApRepository = arApRepository;
    }

    @Override
    public Result<CashFlowForecastVO> forecast(CashFlowForecastDTO dto) {
        String startPeriod = dto != null && hasText(dto.getStartPeriod()) ? dto.getStartPeriod() : "2026-06";
        int months = dto != null && dto.getMonths() != null ? dto.getMonths().intValue() : 3;
        if (months < 1) {
            months = 1;
        }
        if (months > 12) {
            months = 12;
        }
        String scenario = dto != null && hasText(dto.getScenario()) ? dto.getScenario() : "base";
        String currency = dto != null && hasText(dto.getCurrency()) ? dto.getCurrency() : "CNY";

        BigDecimal currentCash = currentBankBalance();
        BigDecimal opening = currentCash;
        BigDecimal lowest = currentCash;
        List<CashFlowForecastVO.ForecastRow> rows = new ArrayList<CashFlowForecastVO.ForecastRow>();
        List<String> alerts = new ArrayList<String>();

        for (int i = 0; i < months; i++) {
            String period = plusMonths(startPeriod, i);
            BigDecimal inflow = forecastInflow(i, scenario);
            BigDecimal outflow = forecastOutflow(i, scenario);
            BigDecimal ending = opening.add(inflow).subtract(outflow);
            String warningLevel = warningLevel(ending);
            String explanation = explanation(warningLevel, inflow, outflow);
            rows.add(new CashFlowForecastVO.ForecastRow(period, opening, inflow, outflow, ending, warningLevel, explanation));
            if (!"GREEN".equals(warningLevel)) {
                alerts.add(period + " 预测期末余额 " + ending + "，低于安全水位 " + SAFETY_LEVEL + "，预警级别 " + warningLevel + "。");
            }
            if (ending.compareTo(lowest) < 0) {
                lowest = ending;
            }
            opening = ending;
        }

        CashFlowForecastVO vo = new CashFlowForecastVO();
        vo.setStartPeriod(startPeriod);
        vo.setScenario(scenario);
        vo.setCurrency(currency);
        vo.setCurrentCashBalance(currentCash);
        vo.setSafetyCashLevel(SAFETY_LEVEL);
        vo.setForecastRows(rows);
        vo.setForecastEndingBalance(rows.get(rows.size() - 1).getEndingBalance());
        vo.setLowestBalance(lowest);
        vo.setLiquidityLevel(liquidityLevel(lowest));
        vo.setAccountPositions(accountPositions());
        vo.setAlerts(alerts);
        vo.setTransferAdvices(transferAdvices(vo));
        vo.setFinancingSuggestions(financingSuggestions(vo));
        vo.setExternalFactors(Arrays.asList(
                "利率：短期流动资金贷款利率按 3.45% 测算，若融资窗口低于 30 天建议优先使用授信额度。",
                "汇率：暂无大额外币收支暴露，暂不触发套期保值建议。",
                "宏观：PMI 低于荣枯线时建议下调销售回款乐观系数 5%。"
        ));
        return Result.success(vo);
    }

    private BigDecimal currentBankBalance() {
        BigDecimal balance = bankRepository.latestBalance("6222-0001");
        return balance.compareTo(BigDecimal.ZERO) > 0 ? balance : new BigDecimal("988400.00");
    }

    private BigDecimal forecastInflow(int monthIndex, String scenario) {
        ArDashboardVO ar = arApRepository.dashboard("2026-05");
        BigDecimal monthlyCollection = ar.getCollectedAmount().multiply(new BigDecimal("0.42"));
        BigDecimal factor = scenarioFactor(scenario);
        BigDecimal seasonal = BigDecimal.ONE.add(new BigDecimal(monthIndex).multiply(new BigDecimal("0.03")));
        return monthlyCollection.multiply(factor).multiply(seasonal).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal forecastOutflow(int monthIndex, String scenario) {
        BigDecimal rigidExpense = new BigDecimal("315000.00");
        BigDecimal supplierPayment = monthIndex == 0 ? new BigDecimal("260000.00") : new BigDecimal("225000.00");
        BigDecimal taxPayment = monthIndex == 0 ? new BigDecimal("75200.00") : new BigDecimal("68000.00");
        BigDecimal growth = BigDecimal.ONE.add(new BigDecimal(monthIndex).multiply(new BigDecimal("0.02")));
        BigDecimal scenarioExtra = "stress".equalsIgnoreCase(scenario) ? new BigDecimal("90000.00") : BigDecimal.ZERO;
        return rigidExpense.add(supplierPayment).add(taxPayment).multiply(growth).add(scenarioExtra).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scenarioFactor(String scenario) {
        if ("optimistic".equalsIgnoreCase(scenario) || "growth".equalsIgnoreCase(scenario)) {
            return new BigDecimal("1.18");
        }
        if ("stress".equalsIgnoreCase(scenario) || "conservative".equalsIgnoreCase(scenario)) {
            return new BigDecimal("0.82");
        }
        return BigDecimal.ONE;
    }

    private String warningLevel(BigDecimal ending) {
        if (ending.compareTo(new BigDecimal("300000.00")) < 0) {
            return "RED";
        }
        if (ending.compareTo(SAFETY_LEVEL) < 0) {
            return "ORANGE";
        }
        if (ending.compareTo(SAFETY_LEVEL.multiply(new BigDecimal("1.25"))) < 0) {
            return "YELLOW";
        }
        return "GREEN";
    }

    private String liquidityLevel(BigDecimal lowest) {
        if (lowest.compareTo(new BigDecimal("300000.00")) < 0) {
            return "TIGHT";
        }
        if (lowest.compareTo(SAFETY_LEVEL) < 0) {
            return "WARNING";
        }
        return "HEALTHY";
    }

    private String explanation(String warningLevel, BigDecimal inflow, BigDecimal outflow) {
        if ("GREEN".equals(warningLevel)) {
            return "经营回款可覆盖刚性支出，资金安全垫充足。";
        }
        if ("YELLOW".equals(warningLevel)) {
            return "期末余额接近安全水位，建议压降非刚性付款。";
        }
        if ("ORANGE".equals(warningLevel)) {
            return "期末余额低于安全水位，需提前安排账户调拨或短期授信。";
        }
        return "现金流缺口明显，预测流出 " + outflow + " 高于流入 " + inflow + "，建议立即启动融资预案。";
    }

    private List<CashFlowForecastVO.AccountPosition> accountPositions() {
        List<CashFlowForecastVO.AccountPosition> rows = new ArrayList<CashFlowForecastVO.AccountPosition>();
        rows.add(new CashFlowForecastVO.AccountPosition("6222-0001", "基本户", new BigDecimal("988400.00"), new BigDecimal("938400.00"), "NORMAL", "保留日常支付资金。"));
        rows.add(new CashFlowForecastVO.AccountPosition("6222-0002", "纳税专户", new BigDecimal("186000.00"), new BigDecimal("168000.00"), "LOW", "税款缴纳前从基本户调入 120000.00。"));
        rows.add(new CashFlowForecastVO.AccountPosition("6222-0003", "保证金户", new BigDecimal("420000.00"), new BigDecimal("60000.00"), "LOCKED", "保证金受限，不能纳入短期可用头寸。"));
        return rows;
    }

    private List<String> transferAdvices(CashFlowForecastVO vo) {
        List<String> advices = new ArrayList<String>();
        advices.add("从基本户向纳税专户预调拨 120000.00，覆盖下期税款支付。 ");
        if (vo.getLowestBalance().compareTo(SAFETY_LEVEL) < 0) {
            advices.add("暂停非刚性供应商提前付款，将 225000.00 付款延后至回款到账后执行。 ");
        } else {
            advices.add("基本户余额高于安全水位，可保留 300000.00 活期头寸，其余用于短期理财或提前付款折扣。 ");
        }
        return advices;
    }

    private List<String> financingSuggestions(CashFlowForecastVO vo) {
        if (vo.getLowestBalance().compareTo(SAFETY_LEVEL) >= 0) {
            return Arrays.asList("未来预测期未出现资金缺口，暂不需要新增融资。", "可评估 7 天通知存款或低风险现金管理产品，提高闲置资金收益。");
        }
        BigDecimal gap = SAFETY_LEVEL.subtract(vo.getLowestBalance()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return Arrays.asList(
                "建议准备不少于 " + gap + " 的短期流动资金授信额度，覆盖最低现金缺口。",
                "优先使用银行循环授信；若缺口持续超过 30 天，可评估票据贴现或供应链金融。"
        );
    }

    private String plusMonths(String period, int offset) {
        String[] parts = period.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int value = year * 12 + month - 1 + offset;
        int newYear = value / 12;
        int newMonth = value % 12 + 1;
        return newYear + "-" + (newMonth < 10 ? "0" + newMonth : String.valueOf(newMonth));
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
