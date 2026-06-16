package com.zcy.finance.mcp.tool;

import java.util.List;
import java.util.Map;

public interface ArgumentMapper {

    List<Object> map(Map<String, Object> arguments);
}
