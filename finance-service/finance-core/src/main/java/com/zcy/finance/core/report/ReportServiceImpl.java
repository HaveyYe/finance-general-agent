package com.zcy.finance.core.report;

import com.zcy.finance.api.ReportService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.ReportGenerateDTO;
import com.zcy.finance.api.vo.BalanceSheetVO;
import com.zcy.finance.api.vo.CashFlowVO;
import com.zcy.finance.api.vo.IncomeStatementVO;
import com.zcy.finance.api.vo.ReportVO;
import com.zcy.finance.infra.report.ReportRepository;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.cache.annotation.Cacheable;

@DubboService(interfaceClass = ReportService.class)
public class ReportServiceImpl implements ReportService {

    private final ReportRepository repository;

    public ReportServiceImpl(ReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<ReportVO> generate(ReportGenerateDTO dto) {
        if (dto == null || dto.getReportType() == null || dto.getPeriod() == null) {
            return Result.failure("报表类型和期间不能为空");
        }
        if ("income_statement".equals(dto.getReportType())) {
            return Result.success((ReportVO) incomeStatement(dto.getPeriod()));
        }
        if ("cash_flow".equals(dto.getReportType())) {
            return Result.success((ReportVO) cashFlowStatement(dto.getPeriod()));
        }
        return Result.success((ReportVO) balanceSheet(dto.getPeriod()));
    }

    @Override
    @Cacheable(cacheNames = "balanceSheet", key = "#period")
    public Result<BalanceSheetVO> getBalanceSheet(String period) {
        if (isBlank(period)) {
            return Result.failure("会计期间不能为空");
        }
        return Result.success(balanceSheet(period));
    }

    @Override
    @Cacheable(cacheNames = "incomeStatement", key = "#period")
    public Result<IncomeStatementVO> getIncomeStatement(String period) {
        if (isBlank(period)) {
            return Result.failure("会计期间不能为空");
        }
        return Result.success(incomeStatement(period));
    }

    @Override
    @Cacheable(cacheNames = "cashFlowStatement", key = "#period")
    public Result<CashFlowVO> getCashFlowStatement(String period) {
        if (isBlank(period)) {
            return Result.failure("会计期间不能为空");
        }
        return Result.success(cashFlowStatement(period));
    }

    private BalanceSheetVO balanceSheet(String period) {
        return repository.balanceSheet(period);
    }

    private IncomeStatementVO incomeStatement(String period) {
        return repository.incomeStatement(period);
    }

    private CashFlowVO cashFlowStatement(String period) {
        return repository.cashFlow(period);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
