package com.zcy.finance.mcp.controller;

import com.zcy.finance.mcp.dto.JsonRpcRequest;
import com.zcy.finance.mcp.dto.JsonRpcResponse;
import com.zcy.finance.mcp.config.FinanceMcpProperties;
import com.zcy.finance.mcp.service.McpJsonRpcService;
import com.zcy.finance.mcp.service.McpRequestContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private final McpJsonRpcService mcpJsonRpcService;
    private final FinanceMcpProperties properties;

    public McpController(McpJsonRpcService mcpJsonRpcService, FinanceMcpProperties properties) {
        this.mcpJsonRpcService = mcpJsonRpcService;
        this.properties = properties;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest request, HttpServletRequest httpRequest) {
        FinanceMcpProperties.Identity identity = properties.getIdentity();
        McpRequestContext context = new McpRequestContext(
                header(httpRequest, "X-Trace-Id"),
                header(httpRequest, identity == null ? "X-Finance-Token" : identity.getTokenHeader()),
                header(httpRequest, identity == null ? "X-Operator-Id" : identity.getOperatorIdHeader()),
                header(httpRequest, identity == null ? "X-Operator-Name" : identity.getOperatorNameHeader())
        );
        return mcpJsonRpcService.handle(request, context);
    }

    private String header(HttpServletRequest request, String name) {
        return name == null ? null : request.getHeader(name);
    }
}
