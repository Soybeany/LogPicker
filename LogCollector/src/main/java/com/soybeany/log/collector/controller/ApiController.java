package com.soybeany.log.collector.controller;

import com.soybeany.log.collector.service.QueryService;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.ResultVO;
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
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private QueryService queryService;

    @GetMapping("/query")
    public String query(@RequestParam Map<String, String> param) {
        try {
            ResultVO result = queryService.query(param);
            return "匹配到:" + result.logResults.size();
        } catch (LogException e) {
            return "异常:" + e.getMessage();
        }
    }

}
