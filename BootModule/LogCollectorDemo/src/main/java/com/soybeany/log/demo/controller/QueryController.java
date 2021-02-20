package com.soybeany.log.demo.controller;

import com.soybeany.log.collector.LogCollector;
import com.soybeany.log.collector.query.QueryService;
import com.soybeany.log.core.model.Direction;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.demo.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
@RestController
@RequestMapping("/query")
public class QueryController {

    @Autowired
    private AppConfig appConfig;

    private QueryService queryService;

    @PostMapping("/byParam")
    public String byParam(@RequestParam Map<String, String> param) {
        try {
            return queryService.simpleQuery(param);
        } catch (LogException e) {
            return "出现异常:" + e.getMessage();
        }
    }

    @PostMapping(value = "/forDirectRead", produces = MediaType.APPLICATION_JSON_VALUE)
    public String forDirectRead(@RequestParam Map<String, String> param) {
        param.put("exporter-exportType", "forDirectRead");
        return byParam(param);
    }

    @PostMapping(value = "/forPack", produces = MediaType.APPLICATION_JSON_VALUE)
    public String forPack(@RequestParam Map<String, String> param) {
        param.put("exporter-exportType", "forPack");
        return byParam(param);
    }

    @GetMapping("/help")
    public String help() {
        return Direction.query("/query/help", "/query/forDirectRead");
    }

    @PostConstruct
    private void onInit() {
        queryService = LogCollector.query(appConfig.toLogCollectConfig()).build();
    }

}
