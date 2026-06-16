package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.BudgetControlQueryDTO;
import com.zcy.finance.api.vo.BudgetControlVO;

public interface BudgetControlService {

    Result<BudgetControlVO> evaluate(BudgetControlQueryDTO dto);
}
