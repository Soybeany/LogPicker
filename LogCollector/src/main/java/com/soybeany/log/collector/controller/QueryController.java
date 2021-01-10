package com.soybeany.log.collector.controller;

import com.google.gson.Gson;
import com.soybeany.log.collector.service.QueryService;
import com.soybeany.log.core.model.LogException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
@RestController
@RequestMapping("/query")
public class QueryController {

    @Autowired
    private QueryService queryService;

    @GetMapping("/byParam")
    public String byParam(@RequestParam Map<String, String> param) {
        try {
            Object result = queryService.simpleQuery(param);
            return new Gson().toJson(result);
        } catch (LogException e) {
            return "异常:" + e.getMessage();
        }
    }

}
