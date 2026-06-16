package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.CashFlowForecastDTO;
import com.zcy.finance.api.vo.CashFlowForecastVO;

public interface CashManagementService {

    Result<CashFlowForecastVO> forecast(CashFlowForecastDTO dto);
}
