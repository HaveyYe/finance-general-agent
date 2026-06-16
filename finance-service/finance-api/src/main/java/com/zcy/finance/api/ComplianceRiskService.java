package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.ComplianceRiskQueryDTO;
import com.zcy.finance.api.vo.ComplianceRiskVO;

public interface ComplianceRiskService {

    Result<ComplianceRiskVO> assess(ComplianceRiskQueryDTO dto);
}
