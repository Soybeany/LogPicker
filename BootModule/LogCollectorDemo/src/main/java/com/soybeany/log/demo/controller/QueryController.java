package com.soybeany.log.demo.controller;

import com.soybeany.log.collector.LogCollector;
import com.soybeany.log.core.model.Direction;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.demo.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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

    @PostMapping("/byParam")
    public String byParam(@RequestParam Map<String, String> param) {
        try {
            return LogCollector.query(appConfig.toLogCollectConfig()).build().simpleQuery(param);
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

    @PostMapping(value = "/forRaw", produces = MediaType.TEXT_PLAIN_VALUE)
    public String forRaw(@RequestParam Map<String, String> param) {
        param.put("exporter-exportType", "forRaw");
        return byParam(param);
    }

    @GetMapping("/config")
    public String config() {
        return ""
                + "待扫描的目录: " + Arrays.toString(appConfig.dirsToScan)
                + "存放索引的目录: " + appConfig.dirForIndexes
                + "当天日志文件的命名: " + appConfig.logTodayFileName
                + "历史日志文件的命名: " + appConfig.logHistoryFileName
                + "日志的字符集: " + appConfig.logCharset
                + "行解析的正则: " + appConfig.lineParseRegex
                + "标签解析的正则: " + appConfig.tagParseRegex
                + "行的时间格式: " + appConfig.lineTimeFormat
                + "建立索引的标签: " + appConfig.tagsToIndex
                + "新记录与旧记录的字节数阈值: " + appConfig.maxBytesGapToMerge
                + "默认一次查询最多返回的结果条数: " + appConfig.defaultMaxResultCount
                + "没有uid时每条查询结果允许包含的最大行数: " + appConfig.maxLinesPerResultWithNoUid;
    }

    @GetMapping("/help")
    public String help() {
        return Direction.QUERY;
    }

}
