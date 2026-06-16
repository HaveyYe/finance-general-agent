package com.zcy.finance.api;

import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.ExpenseApproveDTO;
import com.zcy.finance.api.dto.ExpenseCreateDTO;
import com.zcy.finance.api.dto.ExpenseQueryDTO;
import com.zcy.finance.api.vo.BudgetRemainingVO;
import com.zcy.finance.api.vo.ExpenseApprovalVO;
import com.zcy.finance.api.vo.ExpenseVO;

public interface ExpenseService {

    Result<ExpenseVO> createExpense(ExpenseCreateDTO dto);

    Result<PageResult<ExpenseVO>> query(ExpenseQueryDTO dto);

    Result<ExpenseApprovalVO> approve(ExpenseApproveDTO dto);

    Result<ExpenseApprovalVO> approveExpense(ExpenseApproveDTO dto);

    Result<BudgetRemainingVO> queryBudgetRemaining(String department, String period);
}
