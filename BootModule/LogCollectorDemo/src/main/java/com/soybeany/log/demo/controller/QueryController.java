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
import com.soybeany.log.core.model.MemDataHolder;
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

    private final GsonLogExporter directReadLogExporter = new DirectReadLogExporter();
    private final GsonLogExporter packLogExporter = new PackLogExporter();
    private QueryService queryService;

    @PostMapping(value = "/forDirectRead", produces = MediaType.APPLICATION_JSON_VALUE)
    public String forDirectRead(@RequestParam Map<String, String> param) {
        return byParam(param, directReadLogExporter);
    }

    @PostMapping(value = "/forPack", produces = MediaType.APPLICATION_JSON_VALUE)
    public String forPack(@RequestParam Map<String, String> param) {
        return byParam(param, packLogExporter);
    }

    @GetMapping("/help")
    public String help() {
        return Direction.query("/query/help", "/query/forDirectRead");
    }

    @PostConstruct
    private void onInit() {
        LogCollectConfig config = appConfig.toLogCollectConfig();
        FileProvider fileProvider = new DayBasedRollingFileProvider(appConfig.dirToScan, appConfig.logTodayFileName, appConfig.logHistoryFileName);
        queryService = LogCollector.query(config).build(fileProvider, new MemDataHolder<>(config.maxFileIndexesRetain), new MemDataHolder<>(config.maxResultRetain));
    }

    private String byParam(@RequestParam Map<String, String> param, GsonLogExporter exporter) {
        try {
            return queryService.simpleQuery(param, exporter);
        } catch (LogException e) {
            return "出现异常:" + e.getMessage();
        }
    }

}
