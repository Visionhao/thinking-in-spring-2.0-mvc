package com.vincent.spring.framework.webmvc.servlet;

import com.vincent.spring.framework.annotation.VincentController;
import com.vincent.spring.framework.annotation.VincentRequestMapping;
import com.vincent.spring.framework.context.VincentApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 委派模式
 * 职责： 负责任务调度，请求分发
 * @author vincent
 */
public class VincentDispatcherServlet extends HttpServlet {

    private VincentApplicationContext applicationContext;

    private List<VincentHandlerMapping> handlerMappings = new ArrayList<VincentHandlerMapping>();

    private Map<VincentHandlerMapping,VincentHandlerAdapter> handlerAdapters = new HashMap<VincentHandlerMapping, VincentHandlerAdapter>();

    private List<VincentViewResolver> viewResolvers = new ArrayList<VincentViewResolver>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //6、委派,根据URL去找到一个对应的Method并通过response返回
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            try {
                processDispatchResult(req,resp,new VincentModelAndView("500"));
            } catch (Exception e1) {
                e1.printStackTrace();
                resp.getWriter().write("500 Exception,Detail : " + Arrays.toString(e.getStackTrace()));
            }
        }

    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        //完成了对HandlerMapping的封装
        //完成了对方法返回值的封装ModelAndView

        //1、通过URL获得一个HandlerMapping
        VincentHandlerMapping handler = getHandler(req);
        if(handler == null){
            processDispatchResult(req,resp,new VincentModelAndView("404"));
            return;
        }

        //2、根据一个HandlerMaping获得一个HandlerAdapter
        VincentHandlerAdapter ha = getHandlerAdapter(handler);

        //3、解析某一个方法的形参和返回值之后，统一封装为ModelAndView对象
        VincentModelAndView mv = ha.handler(req,resp,handler);

        // 就把ModelAndView变成一个ViewResolver
        processDispatchResult(req,resp,mv);

    }

    private VincentHandlerAdapter getHandlerAdapter(VincentHandlerMapping handler) {
        if(this.handlerAdapters.isEmpty()){return null;}
        return this.handlerAdapters.get(handler);
    }

    private void processDispatchResult(HttpServletRequest req, HttpServletResponse resp, VincentModelAndView mv) throws Exception {
        if(null == mv){return;}
        if(this.viewResolvers.isEmpty()){return;}

        for (VincentViewResolver viewResolver : this.viewResolvers) {
            VincentView view = viewResolver.resolveViewName(mv.getViewName());
            //直接往浏览器输出
            view.render(mv.getModel(),req,resp);
            return;
        }
    }

    private VincentHandlerMapping getHandler(HttpServletRequest req) {
        if(this.handlerMappings.isEmpty()){return  null;}
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");

        for (VincentHandlerMapping mapping : handlerMappings) {
            Matcher matcher = mapping.getPattern().matcher(url);
            if(!matcher.matches()){continue;}
            return mapping;
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //初始化Spring核心IoC容器
        applicationContext = new VincentApplicationContext(config.getInitParameter("contextConfigLocation"));

        //完成了IoC、DI和MVC部分对接

        //初始化九大组件
        initStrategies(applicationContext);

        System.out.println("Vincent Spring framework is init.");
    }

    private void initStrategies(VincentApplicationContext context) {
//        //多文件上传的组件
//        initMultipartResolver(context);
//        //初始化本地语言环境
//        initLocaleResolver(context);
//        //初始化模板处理器
//        initThemeResolver(context);
        //handlerMapping
        initHandlerMappings(context);
        //初始化参数适配器
        initHandlerAdapters(context);
//        //初始化异常拦截器
//        initHandlerExceptionResolvers(context);
//        //初始化视图预处理器
//        initRequestToViewNameTranslator(context);
        //初始化视图转换器
        initViewResolvers(context);
//        //FlashMap管理器
//        initFlashMapManager(context);
    }

    private void initViewResolvers(VincentApplicationContext context) {
        String templateRoot = context.getConfig().getProperty("templateRoot");
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();

        File templateRootDir = new File(templateRootPath);
        for (File file : templateRootDir.listFiles()) {
            this.viewResolvers.add(new VincentViewResolver(templateRoot));
        }
    }

    private void initHandlerAdapters(VincentApplicationContext context) {
        for (VincentHandlerMapping handlerMapping : handlerMappings) {
            this.handlerAdapters.put(handlerMapping,new VincentHandlerAdapter());
        }
    }

    private void initHandlerMappings(VincentApplicationContext context) {
        if(this.applicationContext.getBeanDefinitionCount() == 0){ return;}

        for (String beanName : this.applicationContext.getBeanDefinitionNames()) {
            Object instance = applicationContext.getBean(beanName);
            Class<?> clazz = instance.getClass();

            if(!clazz.isAnnotationPresent(VincentController.class)){ continue; }

            //相当于提取 class上配置的url
            String baseUrl = "";
            if(clazz.isAnnotationPresent(VincentRequestMapping.class)){
                VincentRequestMapping requestMapping = clazz.getAnnotation(VincentRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //只获取public的方法
            for (Method method : clazz.getMethods()) {
                if(!method.isAnnotationPresent(VincentRequestMapping.class)){continue;}
                //提取每个方法上面配置的url
                VincentRequestMapping requestMapping = method.getAnnotation(VincentRequestMapping.class);

                // //demo//query
                String regex = ("/" + baseUrl + "/" + requestMapping.value().replaceAll("\\*",".*")).replaceAll("/+","/");
                Pattern pattern = Pattern.compile(regex);
                //handlerMapping.put(url,method);
                handlerMappings.add(new VincentHandlerMapping(pattern,instance,method));
                System.out.println("Mapped : " + regex + "," + method);
            }

        }
    }
}
