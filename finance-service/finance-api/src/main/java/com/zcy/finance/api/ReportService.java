package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.ReportGenerateDTO;
import com.zcy.finance.api.vo.BalanceSheetVO;
import com.zcy.finance.api.vo.CashFlowVO;
import com.zcy.finance.api.vo.IncomeStatementVO;
import com.zcy.finance.api.vo.ReportVO;

public interface ReportService {

    Result<ReportVO> generate(ReportGenerateDTO dto);

    Result<BalanceSheetVO> getBalanceSheet(String period);

    Result<IncomeStatementVO> getIncomeStatement(String period);

    Result<CashFlowVO> getCashFlowStatement(String period);
}
