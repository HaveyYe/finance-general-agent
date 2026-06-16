package com.zcy.finance.mcp.dto;

public class JsonRpcResponse {

    private String jsonrpc = "2.0";
    private Object id;
    private Object result;
    private JsonRpcError error;

    public static JsonRpcResponse ok(Object id, Object result) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setResult(result);
        return response;
    }

    public static JsonRpcResponse err(Object id, int code, String message, Object data) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setError(new JsonRpcError(code, message, data));
        return response;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }
}
