package com.zcy.finance.mcp.tool;

import java.util.Collections;
import java.util.List;

public class DubboMapping {

    private final String interfaceName;
    private final String methodName;
    private final List<String> parameterTypes;
    private final ArgumentMapper argumentMapper;

    public DubboMapping(String interfaceName, String methodName, List<String> parameterTypes, ArgumentMapper argumentMapper) {
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypes = Collections.unmodifiableList(parameterTypes);
        this.argumentMapper = argumentMapper;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public ArgumentMapper getArgumentMapper() {
        return argumentMapper;
    }
}
