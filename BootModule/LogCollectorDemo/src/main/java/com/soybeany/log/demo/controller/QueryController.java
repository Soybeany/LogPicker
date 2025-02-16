package com.soybeany.log.demo.controller;

import com.soybeany.log.collector.LogCollector;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.query.exporter.DirectReadLogExporter;
import com.soybeany.log.collector.query.exporter.GsonLogExporter;
import com.soybeany.log.collector.query.exporter.PackLogExporter;
import com.soybeany.log.collector.query.provider.DayBasedRollingFileProvider;
import com.soybeany.log.collector.query.provider.FileProvider;
import com.soybeany.log.collector.query.service.QueryService;
import com.soybeany.log.core.model.Direction;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.demo.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
@RestController
@RequestMapping("/query")
public class QueryController {

    @Autowired
    private AppConfig appConfig;

    private final GsonLogExporter directReadLogExporter = new DirectReadLogExporter();
    private final GsonLogExporter packLogExporter = new PackLogExporter();
    private QueryService queryService;

    @PostMapping(value = "/forDirectRead", produces = MediaType.APPLICATION_JSON_VALUE)
    public String forDirectRead(HttpServletRequest request) {
        return byParam(request, directReadLogExporter);
    }

    @PostMapping(value = "/forPack", produces = MediaType.APPLICATION_JSON_VALUE)
    public String forPack(HttpServletRequest request) {
        return byParam(request, packLogExporter);
    }

    @GetMapping("/help")
    public String help() {
        Direction direction = new Direction();
        return direction.defaultIntroduce()
                .defaultParams()
                .defaultResults()
                .defaultSpecials()
                .defaultTimeSupports()
                .build();
    }

    @PostConstruct
    private void onInit() {
        LogCollectConfig config = appConfig.toLogCollectConfig();
        FileProvider fileProvider = new DayBasedRollingFileProvider(appConfig.dirToScan, appConfig.logTodayFileName, appConfig.logHistoryFileName);
        queryService = LogCollector.query(config).build(fileProvider);
    }

    @PreDestroy
    private void onDestroy() throws IOException {
        queryService.close();
    }

    private String byParam(HttpServletRequest request, GsonLogExporter exporter) {
        try {
            return queryService.simpleQueryWithMultiValueParam(request.getParameterMap(), exporter);
        } catch (LogException e) {
            return "出现异常:" + e.getMessage();
        }
    }

}
