package com.soybeany.log.collector.controller;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.query.QueryService;
import com.soybeany.log.core.model.LogException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @Autowired
    private QueryService queryService;

    @GetMapping("/byParam")
    public String byParam(@RequestParam Map<String, String> param) {
        try {
            return queryService.simpleQuery(param);
        } catch (LogException e) {
            return "出现异常:" + e.getMessage();
        }
    }

    @GetMapping(value = "/forDirectRead", produces = MediaType.APPLICATION_JSON_VALUE)
    public String forDirectRead(@RequestParam Map<String, String> param) {
        param.put("exporter-exportType", "forDirectRead");
        return byParam(param);
    }

    @GetMapping(value = "/forRead", produces = MediaType.APPLICATION_JSON_VALUE)
    public String forRead(@RequestParam Map<String, String> param) {
        param.put("exporter-exportType", "forRead");
        return byParam(param);
    }

    @GetMapping(value = "/forRead", produces = MediaType.TEXT_PLAIN_VALUE)
    public String inSerialize(@RequestParam Map<String, String> param) {
        param.put("exporter-exportType", "inSerialize");
        return byParam(param);
    }

    @GetMapping("/help")
    public String help() {
        return ""
                + "查询建议"
                + "\n  1.指定时间查询区间"
                + "\n  2.使用索引型的标签参数"
                + "\n  3.按需设置结果条数限制"
                + "\n\n标准参数"
                + "\n  fromTime: 开始时间，支持下方所列的多种时间格式(默认当天00:00)"
                + "\n  toTime: 结束时间，支持下方所列的多种时间格式(默认当前时间)"
                + "\n  countLimit: 一次查询最多返回的结果条数(默认使用配置值)"
                + "\n  logFiles: 指定待查询的文件全路径，多个时使用“;”或“,”进行分隔(默认根据时间选择文件)"
                + "\n  uidList: 指定待查询的uid，多个时使用“;”或“,”进行分隔(默认不指定)"
                + "\n  result-id: 查询指定id的结果，用于分页查询(默认不指定)"
                + "\n\n标签参数"
                + "\n  格式为“tag-xxx”，如“tag-url”、“tag-user”等，分为“索引型”与“过滤型”两种，在查询结果的msg中能查看"
                + "\n  索引型：扫描时会索引该tag，查询前使用索引锁定查询范围，可极大地提高查询速度"
                + "\n  过滤型：参考过滤参数，不能提高查询速度"
                + "\n  匹配策略为contain，不区分大小写"
                + "\n\n过滤参数"
                + "\n  格式为“filter-xxx”，如“filter-containsKey”，查询后才对该结果进行条件筛选"
                + "\n  containsKey: key包含，不区分大小写"
                + "\n\n支持的时间格式"
                + "\n  1.yyyy-MM-dd HH:mm:ss"
                + "\n  2.yyyy-MM-dd HH:mm"
                + "\n  3.yy-MM-dd HH:mm:ss"
                + "\n  4.yy-MM-dd HH:mm"
                + "\n  5.HH:mm:ss"
                + "\n  6.HH:mm"
                + "\n\n生效中的配置"
                + "\n  待扫描的目录: " + Arrays.toString(appConfig.dirsToScan)
                + "\n  存放索引的目录: " + appConfig.dirForIndexes
                + "\n  当天日志文件的命名: " + appConfig.logTodayFileName
                + "\n  历史日志文件的命名: " + appConfig.logHistoryFileName
                + "\n  日志的字符集: " + appConfig.logCharset
                + "\n  行解析的正则: " + appConfig.lineParsePattern
                + "\n  标签解析的正则: " + appConfig.tagParsePattern
                + "\n  行的时间格式: " + appConfig.lineTimeFormat
                + "\n  建立索引的标签: " + appConfig.tagsToIndex
                + "\n  新记录与旧记录的字节数阈值: " + appConfig.maxBytesGapToMerge
                + "\n  默认一次查询最多返回的结果条数: " + appConfig.defaultMaxResultCount
                + "\n  没有uid时每条查询结果允许包含的最大行数: " + appConfig.maxLinesPerResultWithNoUid
                ;
    }

}
