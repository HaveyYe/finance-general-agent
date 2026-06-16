package com.zcy.finance.mcp.dubbo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zcy.finance.mcp.config.FinanceDubboProperties;
import com.zcy.finance.mcp.dto.DubboGenericCallArgs;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
public class DubboGenericInvokeService {

    private static final String DEFAULT_APP_NAME = "finance-general-mcp";
    private static final int MAX_RETRY = 1;
    private static final long RETRY_INTERVAL_MS = 500L;
    private final ObjectMapper objectMapper;
    private final FinanceDubboProperties properties;

    public DubboGenericInvokeService(ObjectMapper objectMapper, FinanceDubboProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public Object invoke(DubboGenericCallArgs args) {
        validate(args);
        RuntimeException lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
            try {
                return doInvoke(args);
            } catch (RuntimeException e) {
                if (!shouldRetry(e) || attempt == MAX_RETRY) {
                    throw e;
                }
                lastException = e;
                sleepBeforeRetry();
            }
        }

        throw lastException;
    }

    private Object doInvoke(DubboGenericCallArgs args) {
        Class<?> interfaceClass = tryLoadClass(args.getInterfaceName());
        if (interfaceClass != null) {
            return doTypedInvoke(args, interfaceClass);
        }
        return doGenericInvoke(args);
    }

    private Object doTypedInvoke(DubboGenericCallArgs args, Class<?> interfaceClass) {
        ReferenceConfig<Object> reference = new ReferenceConfig<Object>();
        reference.setApplication(new ApplicationConfig(applicationName()));
        reference.setInterface(interfaceClass);
        reference.setCheck(false);
        reference.setRetries(0);

        applyReferenceSettings(reference, args);

        Object service = SimpleReferenceCache.getCache().get(reference);
        Class<?>[] parameterTypes = resolveParameterTypes(args.getParameterTypes());
        Object[] values = convertArguments(args.getArgs(), parameterTypes);
        Method method = findMethod(interfaceClass, args.getMethodName(), parameterTypes);
        try {
            return method.invoke(service, values);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to access method " + args.getMethodName(), e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            }
            throw new IllegalStateException("Failed to invoke method " + args.getMethodName(), target);
        }
    }

    private Object doGenericInvoke(DubboGenericCallArgs args) {
        ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
        reference.setApplication(new ApplicationConfig(applicationName()));
        reference.setInterface(args.getInterfaceName());
        reference.setGeneric("true");
        reference.setCheck(false);
        reference.setRetries(0);

        applyReferenceSettings(reference, args);

        GenericService genericService = SimpleReferenceCache.getCache().get(reference);

        List<String> parameterTypes = args.getParameterTypes();
        List<Object> callArgs = args.getArgs();

        String[] types = parameterTypes.toArray(new String[0]);
        Object[] values = callArgs.toArray();

        return genericService.$invoke(args.getMethodName(), types, values);
    }

    private void applyReferenceSettings(ReferenceConfig<?> reference, DubboGenericCallArgs args) {
        if (StringUtils.hasText(args.getGroup())) {
            reference.setGroup(args.getGroup());
        }
        if (StringUtils.hasText(args.getVersion())) {
            reference.setVersion(args.getVersion());
        }
        if (args.getTimeoutMs() != null && args.getTimeoutMs() > 0) {
            reference.setTimeout(args.getTimeoutMs());
        }
        Map<String, String> parameters = referenceParameters(args);
        if (!parameters.isEmpty()) {
            reference.setParameters(parameters);
        }
        if (StringUtils.hasText(args.getDirectUrl())) {
            reference.setUrl(args.getDirectUrl());
        } else if (StringUtils.hasText(args.getRegistryAddress())) {
            reference.setRegistry(new RegistryConfig(args.getRegistryAddress()));
        }
    }

    private Map<String, String> referenceParameters(DubboGenericCallArgs args) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        if (properties.getReferenceParameters() != null) {
            for (Map.Entry<String, String> entry : properties.getReferenceParameters().entrySet()) {
                if (StringUtils.hasText(entry.getKey()) && StringUtils.hasText(entry.getValue())) {
                    parameters.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (args.getParameters() != null) {
            for (Map.Entry<String, String> entry : args.getParameters().entrySet()) {
                if (StringUtils.hasText(entry.getKey()) && StringUtils.hasText(entry.getValue())) {
                    parameters.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return parameters;
    }

    private String applicationName() {
        if (StringUtils.hasText(properties.getApplicationName())) {
            return properties.getApplicationName();
        }
        return DEFAULT_APP_NAME;
    }

    private Class<?> tryLoadClass(String className) {
        try {
            return resolveClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Class<?>[] resolveParameterTypes(List<String> typeNames) {
        Class<?>[] types = new Class<?>[typeNames.size()];
        for (int i = 0; i < typeNames.size(); i++) {
            try {
                types[i] = resolveClass(typeNames.get(i));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("parameter type not found: " + typeNames.get(i), e);
            }
        }
        return types;
    }

    private Class<?> resolveClass(String className) throws ClassNotFoundException {
        if ("boolean".equals(className)) {
            return boolean.class;
        }
        if ("byte".equals(className)) {
            return byte.class;
        }
        if ("short".equals(className)) {
            return short.class;
        }
        if ("int".equals(className)) {
            return int.class;
        }
        if ("long".equals(className)) {
            return long.class;
        }
        if ("float".equals(className)) {
            return float.class;
        }
        if ("double".equals(className)) {
            return double.class;
        }
        if ("char".equals(className)) {
            return char.class;
        }
        return Class.forName(className);
    }

    private Object[] convertArguments(List<Object> args, Class<?>[] parameterTypes) {
        Object[] values = new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            values[i] = convertArgument(args.get(i), parameterTypes[i]);
        }
        return values;
    }

    private Object convertArgument(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType.isPrimitive()) {
            return objectMapper.convertValue(value, primitiveWrapper(targetType));
        }
        if (isSimpleTarget(targetType) || value instanceof Map) {
            return objectMapper.convertValue(value, targetType);
        }
        return value;
    }

    private Class<?> primitiveWrapper(Class<?> primitiveType) {
        if (primitiveType == boolean.class) {
            return Boolean.class;
        }
        if (primitiveType == byte.class) {
            return Byte.class;
        }
        if (primitiveType == short.class) {
            return Short.class;
        }
        if (primitiveType == int.class) {
            return Integer.class;
        }
        if (primitiveType == long.class) {
            return Long.class;
        }
        if (primitiveType == float.class) {
            return Float.class;
        }
        if (primitiveType == double.class) {
            return Double.class;
        }
        if (primitiveType == char.class) {
            return Character.class;
        }
        return primitiveType;
    }

    private boolean isSimpleTarget(Class<?> targetType) {
        return CharSequence.class.isAssignableFrom(targetType)
                || Number.class.isAssignableFrom(targetType)
                || Boolean.class == targetType
                || Character.class == targetType
                || BigDecimal.class == targetType
                || BigInteger.class == targetType
                || targetType.isEnum();
    }

    private Method findMethod(Class<?> interfaceClass, String methodName, Class<?>[] parameterTypes) {
        try {
            return interfaceClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            for (Method method : interfaceClass.getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterTypes().length == parameterTypes.length) {
                    return method;
                }
            }
            throw new IllegalArgumentException("method not found: " + interfaceClass.getName() + "#" + methodName, e);
        }
    }

    static boolean shouldRetry(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof IllegalArgumentException || current instanceof GenericException) {
                return false;
            }
            if (current instanceof RpcException) {
                RpcException rpcException = (RpcException) current;
                return rpcException.isTimeout() || rpcException.isNetwork();
            }
            if (current instanceof org.apache.dubbo.remoting.TimeoutException
                    || current instanceof TimeoutException
                    || current instanceof SocketTimeoutException
                    || current instanceof ConnectException
                    || current instanceof SocketException
                    || current instanceof RemotingException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void validate(DubboGenericCallArgs args) {
        if (args == null) {
            throw new IllegalArgumentException("arguments is required");
        }
        if (!StringUtils.hasText(args.getInterfaceName())) {
            throw new IllegalArgumentException("interfaceName is required");
        }
        if (!StringUtils.hasText(args.getMethodName())) {
            throw new IllegalArgumentException("methodName is required");
        }
        if (args.getParameterTypes() == null) {
            throw new IllegalArgumentException("parameterTypes is required");
        }
        if (args.getArgs() == null) {
            throw new IllegalArgumentException("args is required");
        }
        if (args.getParameterTypes().size() != args.getArgs().size()) {
            throw new IllegalArgumentException("parameterTypes size must match args size");
        }
        if (!StringUtils.hasText(args.getDirectUrl()) && !StringUtils.hasText(args.getRegistryAddress())) {
            throw new IllegalArgumentException("directUrl or registryAddress is required");
        }
    }
}
