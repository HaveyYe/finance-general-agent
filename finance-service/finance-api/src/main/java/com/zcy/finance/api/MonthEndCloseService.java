package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.MonthEndCloseDTO;
import com.zcy.finance.api.vo.MonthEndCloseVO;

public interface MonthEndCloseService {

    Result<MonthEndCloseVO> runCloseCheck(MonthEndCloseDTO dto);
}
