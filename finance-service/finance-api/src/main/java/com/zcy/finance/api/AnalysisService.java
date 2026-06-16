package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.AnalysisQueryDTO;
import com.zcy.finance.api.dto.VarianceDiagnosisDTO;
import com.zcy.finance.api.vo.AnalysisVO;
import com.zcy.finance.api.vo.AnomalyVO;
import com.zcy.finance.api.vo.FinancialRatioVO;
import com.zcy.finance.api.vo.TrendAnalysisVO;
import com.zcy.finance.api.vo.VarianceDiagnosisVO;

import java.util.List;

public interface AnalysisService {

    Result<AnalysisVO> analyze(AnalysisQueryDTO dto);

    Result<List<FinancialRatioVO>> calculateRatios(String period);

    Result<TrendAnalysisVO> analyzeTrend(String startDate, String endDate, String metric);

    Result<List<AnomalyVO>> detectAnomalies(String period);

    Result<VarianceDiagnosisVO> diagnoseVariance(VarianceDiagnosisDTO dto);
}
