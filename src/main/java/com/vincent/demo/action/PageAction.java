package com.vincent.demo.action;

import com.vincent.demo.service.IQueryService;
import com.vincent.spring.framework.annotation.VincentAutowired;
import com.vincent.spring.framework.annotation.VincentController;
import com.vincent.spring.framework.annotation.VincentRequestMapping;
import com.vincent.spring.framework.annotation.VincentRequestParam;
import com.vincent.spring.framework.webmvc.servlet.VincentModelAndView;

import java.util.HashMap;
import java.util.Map;

@VincentController
@VincentRequestMapping("/")
public class PageAction {

    @VincentAutowired
    IQueryService queryService;

    @VincentRequestMapping("/first.html")
    public VincentModelAndView query(@VincentRequestParam("name") String name){
        String result = queryService.query(name);
        Map<String,Object> model = new HashMap<String, Object>();
        model.put("name",name);
        model.put("data",result);
        model.put("token","abc123");
        return new VincentModelAndView("first.html",model);
    }
}
