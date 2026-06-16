package com.zcy.finance.api;

import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.VoucherAuditDTO;
import com.zcy.finance.api.dto.VoucherCreateDTO;
import com.zcy.finance.api.dto.VoucherQueryDTO;
import com.zcy.finance.api.vo.VoucherAuditVO;
import com.zcy.finance.api.vo.VoucherVO;

public interface VoucherService {

    Result<VoucherVO> createVoucher(VoucherCreateDTO dto);

    Result<VoucherVO> getVoucher(String voucherNo);

    Result<PageResult<VoucherVO>> queryVouchers(VoucherQueryDTO dto);

    Result<Void> auditVoucher(String voucherNo, String auditor);

    Result<VoucherAuditVO> auditVoucher(VoucherAuditDTO dto);
}
