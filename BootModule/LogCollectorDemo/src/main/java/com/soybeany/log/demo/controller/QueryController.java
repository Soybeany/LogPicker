package com.soybeany.log.demo.controller;

import com.soybeany.log.collector.LogCollector;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.query.QueryService;
import com.soybeany.log.collector.query.exporter.DirectReadLogExporter;
import com.soybeany.log.collector.query.exporter.PackLogExporter;
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

    private QueryService directReadQueryService;
    private QueryService packQueryService;

    @PostMapping(value = "/forDirectRead", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object forDirectRead(@RequestParam Map<String, String> param) {
        return byParam(directReadQueryService, param);
    }

    @PostMapping(value = "/forPack", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object forPack(@RequestParam Map<String, String> param) {
        return byParam(packQueryService, param);
    }

    @GetMapping("/help")
    public String help() {
        return Direction.query("/query/help", "/query/forDirectRead");
    }

    @PostConstruct
    private void onInit() {
        LogCollectConfig config = appConfig.toLogCollectConfig();
        directReadQueryService = LogCollector.query(config).logExporter(new DirectReadLogExporter()).build();
        packQueryService = LogCollector.query(config).logExporter(new PackLogExporter()).build();
    }

    private Object byParam(QueryService queryService, @RequestParam Map<String, String> param) {
        try {
            return queryService.simpleQuery(param);
        } catch (LogException e) {
            return "出现异常:" + e.getMessage();
        }
    }

}
