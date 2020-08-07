package com.platform.common.core.aspect;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.platform.common.core.aop.RequestLogIgnore;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 记录接口日志切面
 * @author: 吴邪
 * @date: 2020/6/5 14:26
 */
@Aspect
@Slf4j
@Component
public class LogAspect {

    @Pointcut("execution(* com.platform.*.*.controller.*.*(..))")
    public void pointCut(){}

    @SuppressWarnings("unchecked")
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        RequestLogIgnore annotation = signature.getMethod().getAnnotation(RequestLogIgnore.class);
        annotation = annotation==null?proceedingJoinPoint.getTarget().getClass().getAnnotation(RequestLogIgnore.class):annotation;
        if(annotation!=null){
            proceedingJoinPoint.proceed();
        }

        Map<String, Object> pointCutInfo = getPointCutInfo(proceedingJoinPoint);

        String className = Convert.toStr(pointCutInfo.get("className"));
        String methodName = Convert.toStr(pointCutInfo.get("methodName"));

        Map<Class<?>, Object> args = (Map<Class<?>, Object>) pointCutInfo.get("args");
        log.info("执行开始()->{}.{}({})", className, methodName, getArgsToString(args));
        StopWatch stopWatch = new StopWatch(StrUtil.concat(true,className,".",methodName));
        stopWatch.start();
        Object result = proceedingJoinPoint.proceed();
        stopWatch.stop();
        log.info(stopWatch.shortSummary());

        return result;
    }


    /**
     * 获取连接点信息.
     *
     * @param joinPoint 连接点
     * @return 连接点信息
     */
    private static Map<String, Object> getPointCutInfo(JoinPoint joinPoint) {

        // 类名、方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();

        // 方法形参类型
        StringBuilder methodArgTypes = new StringBuilder();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class[] parameterTypes = signature.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            methodArgTypes.append(parameterTypes[i].getSimpleName());
            if (i != parameterTypes.length - 1) {
                methodArgTypes.append(", ");
            }
        }
        // 方法实参
        Object[] args = joinPoint.getArgs();
        Map<Class<?>, Object> argMap = new LinkedHashMap<>(args.length);

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Class<?> argType = parameterTypes[i];
            argMap.put(argType, arg);
        }


        Map<String, Object> map = new HashMap<>(4);
        map.put("className", className);
        map.put("methodName", methodName);
        map.put("methodArgTypes", methodArgTypes);
        map.put("args", argMap);

        return map;
    }

    private String getArgsToString(Map<Class<?>, Object> argMap) {
        StringBuilder sb = new StringBuilder();
        List<Map.Entry<Class<?>, Object>> args = new ArrayList<>(argMap.entrySet());

        int size = args.size();
        for (int i = 0; i < size; i++) {
            Map.Entry<Class<?>, Object> arg = args.get(i);
            String typeName = arg.getKey().getSimpleName();
            Object value = arg.getValue();
            sb.append(typeName);
            sb.append(":");
            if (value != null) {
                try {
                    sb.append(JSONUtil.toJsonStr(arg.getValue()));
                } catch (Exception ignored) {
                }
            } else {
                sb.append("null");
            }
            if (i != size - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }
}