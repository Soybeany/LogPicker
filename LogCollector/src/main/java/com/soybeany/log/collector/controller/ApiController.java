package com.soybeany.log.collector.controller;

import com.google.gson.Gson;
import com.soybeany.log.collector.service.QueryService;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
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

    public void scan() {

    }

    @GetMapping("/query")
    public String query(@RequestParam Map<String, String> param) {
        try {
            ResultVO result = queryService.query(param);
            return new Gson().toJson(toOutputObj(result));
        } catch (LogException e) {
            return "异常:" + e.getMessage();
        }
    }

    private List<Object> toOutputObj(ResultVO result) {
        List<Object> list = new LinkedList<>();
        list.add(new Detail(result));
        list.addAll(result.logResults);
        return list;
    }

    private static class Detail {
        public final String lastContextId;
        public final String curContextId;
        public final String nextContextId;
        public final String endReason;

        public Detail(ResultVO result) {
            this.lastContextId = result.lastContextId;
            this.curContextId = result.curContextId;
            this.nextContextId = result.nextContextId;
            this.endReason = result.endReason;
        }
    }
}
