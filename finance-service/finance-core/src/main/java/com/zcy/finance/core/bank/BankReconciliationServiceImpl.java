package com.zcy.finance.core.bank;

import com.zcy.finance.api.BankReconciliationService;
import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.BankReconcileDTO;
import com.zcy.finance.api.dto.BankTransactionQueryDTO;
import com.zcy.finance.api.vo.BankReconcileVO;
import com.zcy.finance.api.vo.BankTransactionVO;
import com.zcy.finance.infra.bank.BankTransactionRepository;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DubboService(interfaceClass = BankReconciliationService.class)
public class BankReconciliationServiceImpl implements BankReconciliationService {

    private final BankTransactionRepository repository;

    public BankReconciliationServiceImpl(BankTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<PageResult<BankTransactionVO>> queryTransactions(BankTransactionQueryDTO dto) {
        List<BankTransactionVO> rows = repository.findAll();
        List<BankTransactionVO> filtered = new ArrayList<BankTransactionVO>();
        for (BankTransactionVO row : rows) {
            if (dto != null && hasText(dto.getAccountNo()) && !dto.getAccountNo().equals(row.getAccountNo())) {
                continue;
            }
            if (dto != null && hasText(dto.getStatus()) && !dto.getStatus().equals(row.getStatus())) {
                continue;
            }
            if (dto != null && dto.getDateRange() != null && dto.getDateRange().size() >= 2
                    && (row.getTransactionDate().compareTo(dto.getDateRange().get(0)) < 0
                    || row.getTransactionDate().compareTo(dto.getDateRange().get(1)) > 0)) {
                continue;
            }
            filtered.add(row);
        }
        return Result.success(PageResult.of(filtered, filtered.size(), 1, filtered.size()));
    }

    @Override
    public Result<BankReconcileVO> reconcile(BankReconcileDTO dto) {
        String accountNo = dto != null && hasText(dto.getAccountNo()) ? dto.getAccountNo() : "6222-0001";
        String period = dto != null && hasText(dto.getPeriod()) ? dto.getPeriod() : "2026-05";
        List<BankTransactionVO> rows = repository.findAll();
        List<BankTransactionVO> unmatched = new ArrayList<BankTransactionVO>();
        int matchedCount = 0;
        for (BankTransactionVO row : rows) {
            if (!accountNo.equals(row.getAccountNo())) {
                continue;
            }
            if ("MATCHED".equals(row.getStatus())) {
                matchedCount++;
            } else {
                unmatched.add(row);
            }
        }

        BankReconcileVO vo = new BankReconcileVO();
        vo.setAccountNo(accountNo);
        vo.setPeriod(period);
        vo.setBankBalance(repository.latestBalance(accountNo));
        vo.setLedgerBalance(new BigDecimal("1043200.00"));
        vo.setDifferenceAmount(vo.getBankBalance().subtract(vo.getLedgerBalance()));
        vo.setTotalCount(matchedCount + unmatched.size());
        vo.setMatchedCount(matchedCount);
        vo.setUnmatchedCount(unmatched.size());
        vo.setUnmatchedTransactions(unmatched);
        vo.setAdvices(Arrays.asList(
                "存在 " + unmatched.size() + " 条未达或风险流水，建议优先处理供应商付款和审批中报销。",
                "银行余额低于账面余额，需确认未入账付款、在途回款和凭证生成状态。",
                "可将 UNMATCHED 流水推送给凭证模块生成待复核凭证。"
        ));
        return Result.success(vo);
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
