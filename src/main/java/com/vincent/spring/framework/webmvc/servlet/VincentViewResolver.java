package com.vincent.spring.framework.webmvc.servlet;

import java.io.File;

public class VincentViewResolver {

    private final String DEFAULT_TEMPLATE_SUFFIX = ".html";
    private File tempateRootDir;
    public VincentViewResolver(String templateRoot) {
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        tempateRootDir = new File(templateRootPath);
    }

    public VincentView resolveViewName(String viewName){
        if(null == viewName || "".equals(viewName.trim())){return null;}
        viewName = viewName.endsWith(DEFAULT_TEMPLATE_SUFFIX)? viewName : (viewName + DEFAULT_TEMPLATE_SUFFIX);
        File templateFile = new File((tempateRootDir.getPath() + "/" + viewName).replaceAll("/+","/"));
        return new VincentView(templateFile);
    }
}
