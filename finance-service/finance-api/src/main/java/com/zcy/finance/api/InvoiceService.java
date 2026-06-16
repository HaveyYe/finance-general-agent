package com.zcy.finance.api;

import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.InvoiceInputDTO;
import com.zcy.finance.api.dto.InvoiceQueryDTO;
import com.zcy.finance.api.dto.InvoiceVerifyDTO;
import com.zcy.finance.api.vo.InvoiceCheckVO;
import com.zcy.finance.api.vo.InvoiceDupVO;
import com.zcy.finance.api.vo.InvoiceDuplicateVO;
import com.zcy.finance.api.vo.InvoiceVerificationVO;
import com.zcy.finance.api.vo.InvoiceVO;

import java.util.List;

public interface InvoiceService {

    Result<InvoiceVO> inputInvoice(InvoiceInputDTO dto);

    Result<PageResult<InvoiceVO>> query(InvoiceQueryDTO dto);

    Result<PageResult<InvoiceVO>> queryLedger(InvoiceQueryDTO dto);

    Result<InvoiceCheckVO> checkInvoice(String invoiceCode, String invoiceNo);

    Result<List<InvoiceDuplicateVO>> checkDuplicate(InvoiceInputDTO dto);

    Result<List<InvoiceDupVO>> checkDuplicate(String invoiceCode, String invoiceNo);

    Result<InvoiceVerificationVO> verify(InvoiceVerifyDTO dto);
}
