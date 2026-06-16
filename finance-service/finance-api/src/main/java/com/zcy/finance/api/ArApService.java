package com.zcy.finance.api;

import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.AgingQueryDTO;
import com.zcy.finance.api.vo.AgingVO;
import com.zcy.finance.api.vo.CollectionAdviceVO;
import com.zcy.finance.api.vo.ReconciliationVO;

import java.util.List;

public interface ArApService {

    Result<PageResult<AgingVO>> queryAging(AgingQueryDTO dto);

    Result<List<CollectionAdviceVO>> suggestCollection(String customerId);

    Result<ReconciliationVO> reconcile(String partnerId, String period);
}
