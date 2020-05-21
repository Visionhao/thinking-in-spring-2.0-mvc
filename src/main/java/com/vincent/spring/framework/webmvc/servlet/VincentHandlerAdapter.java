package com.vincent.spring.framework.webmvc.servlet;

import com.vincent.spring.framework.annotation.VincentRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VincentHandlerAdapter {

    public VincentModelAndView handler(HttpServletRequest req, HttpServletResponse resp, VincentHandlerMapping handlerMapping) throws InvocationTargetException, IllegalAccessException {
        //保存形参列表
        //将参数名称和参数的位置，这种关系保存起来
        Map<String,Integer> paramIndexMapping = new HashMap<String, Integer>();

        Annotation[] [] pa = handlerMapping.getMethod().getParameterAnnotations();
        for (int i = 0; i < pa.length; i++) {
            for(Annotation a : pa[i]){
                if(a instanceof VincentRequestParam){
                    String paramName = ((VincentRequestParam) a).value();
                    if(!"".equals(paramName.trim())){
                        paramIndexMapping.put(paramName,i);
                    }
                }
            }
        }

        //初始化一下
        Class<?>[] paramTypes = handlerMapping.getMethod().getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramterType = paramTypes[i];
            if(paramterType == HttpServletRequest.class || paramterType == HttpServletResponse.class){
                paramIndexMapping.put(paramterType.getName(),i);
            }
        }

        //去拼接实参列表
        Map<String,String[]> params = req.getParameterMap();

        Object[] paramValues = new Object[paramTypes.length];

        for(Map.Entry<String,String[]> param : params.entrySet()){
            String value = Arrays.toString(params.get(param.getKey()))
                    .replaceAll("\\[|\\]","")
                    .replaceAll("\\s+",",");

            if(!paramIndexMapping.containsKey(param.getKey())){
                continue;
            }

            int index = paramIndexMapping.get(param.getKey());
            paramValues[index] = castStringValue(value,paramTypes[index]);
        }
        if(paramIndexMapping.containsKey(HttpServletRequest.class.getName())){
            int index  = paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[index] = req;
        }
        if(paramIndexMapping.containsKey(HttpServletResponse.class.getName())){
            int index  = paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[index] = resp;
        }

        Object result = handlerMapping.getMethod().invoke(handlerMapping.getController(),paramValues);
        if(result == null || result instanceof Void){
            return null;
        }
        boolean isModelAndView = handlerMapping.getMethod().getReturnType() == VincentModelAndView.class;
        if(isModelAndView){
            return (VincentModelAndView)result;
        }
        return null;
    }

    private Object castStringValue(String value, Class<?> paramType) {
        if(String.class == paramType){
            return value;
        }else if(Integer.class == paramType){
            return Integer.valueOf(value);
        }else if(Double.class == paramType){
            return Double.valueOf(value);
        }else{
            if(value != null){
                return value;
            }
            return null;
        }
    }
}
