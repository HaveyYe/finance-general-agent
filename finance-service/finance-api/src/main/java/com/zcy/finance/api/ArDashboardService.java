package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.CollectionPlanDTO;
import com.zcy.finance.api.dto.CounterpartyReconcileDTO;
import com.zcy.finance.api.dto.PaymentPlanOptimizeDTO;
import com.zcy.finance.api.vo.ArDashboardVO;
import com.zcy.finance.api.vo.CollectionPlanVO;
import com.zcy.finance.api.vo.CounterpartyReconcileVO;
import com.zcy.finance.api.vo.PaymentPlanVO;

public interface ArDashboardService {

    Result<ArDashboardVO> queryOverview(String month);

    Result<CollectionPlanVO> generateCollectionPlan(CollectionPlanDTO dto);

    Result<PaymentPlanVO> optimizePaymentPlan(PaymentPlanOptimizeDTO dto);

    Result<CounterpartyReconcileVO> reconcileCounterparty(CounterpartyReconcileDTO dto);
}
