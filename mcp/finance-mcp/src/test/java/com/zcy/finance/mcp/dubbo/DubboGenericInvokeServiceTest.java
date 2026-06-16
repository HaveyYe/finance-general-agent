package com.zcy.finance.mcp.dubbo;

import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.service.GenericException;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DubboGenericInvokeServiceTest {

    @Test
    void retriesTimeoutAndNetworkFailures() {
        assertTrue(DubboGenericInvokeService.shouldRetry(
                new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout")));
        assertTrue(DubboGenericInvokeService.shouldRetry(
                new RpcException(RpcException.NETWORK_EXCEPTION, "network")));
        assertTrue(DubboGenericInvokeService.shouldRetry(
                new RuntimeException(new TimeoutException("timeout"))));
        assertTrue(DubboGenericInvokeService.shouldRetry(
                new RuntimeException(new ConnectException("connection refused"))));
    }

    @Test
    void doesNotRetryBusinessOrUnknownFailures() {
        assertFalse(DubboGenericInvokeService.shouldRetry(new IllegalArgumentException("invalid")));
        assertFalse(DubboGenericInvokeService.shouldRetry(new GenericException("biz", "failed")));
        assertFalse(DubboGenericInvokeService.shouldRetry(
                new RpcException(RpcException.BIZ_EXCEPTION, "business failed")));
        assertFalse(DubboGenericInvokeService.shouldRetry(new RuntimeException("unknown")));
    }
}
