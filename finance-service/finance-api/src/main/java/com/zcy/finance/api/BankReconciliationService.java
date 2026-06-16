package com.zcy.finance.api;

import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.BankTransactionQueryDTO;
import com.zcy.finance.api.dto.BankReconcileDTO;
import com.zcy.finance.api.vo.BankReconcileVO;
import com.zcy.finance.api.vo.BankTransactionVO;

public interface BankReconciliationService {

    Result<PageResult<BankTransactionVO>> queryTransactions(BankTransactionQueryDTO dto);

    Result<BankReconcileVO> reconcile(BankReconcileDTO dto);
}
