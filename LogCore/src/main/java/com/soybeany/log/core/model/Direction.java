package com.soybeany.log.core.model;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/2/8
 */
public class Direction {

    public static final String TITLE_PARAMS_STD = "入参-标准(无固定前缀)";
    public static final String TITLE_PARAMS_TAG = "入参-标签(格式为“tag-xxx”)，分为“索引型[快]”与“过滤型[慢]”两种";
    public static final String TITLE_PARAMS_FILTER = "入参-过滤(格式为“filter-xxx”)，在索引范围内执行[慢]";
    public static final String TITLE_PARAMS_EXPORTER = "入参-导出器(格式为“exporter-xxx”)，在过滤结果中执行[快]";

    public static final String TITLE_RESULTS_INFO = "结果-概要";
    public static final String TITLE_RESULTS_ITEM = "结果-条目";

    private final List<String> introduces = new ArrayList<>();
    private final Map<String, Map<String, String>> params = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> results = new LinkedHashMap<>();
    private final List<String> specials = new ArrayList<>();
    private final Map<String, List<String>> others = new LinkedHashMap<>();

    public Direction addSuggestions(String... values) {
        introduces.addAll(Arrays.asList(values));
        return this;
    }

    public Direction addParams(String type, Map<String, String> values) {
        params.computeIfAbsent(type, k -> new LinkedHashMap<>()).putAll(values);
        return this;
    }

    public Direction addResults(String type, Map<String, String> values) {
        results.computeIfAbsent(type, k -> new LinkedHashMap<>()).putAll(values);
        return this;
    }

    public Direction addSpecials(String... values) {
        specials.addAll(Arrays.asList(values));
        return this;
    }

    public Direction addOthers(String title, String... values) {
        others.computeIfAbsent(title, k -> new ArrayList<>()).addAll(Arrays.asList(values));
        return this;
    }

    public Direction addOthers(String title, Map<String, String> values) {
        List<String> list = new ArrayList<>();
        appendKv(list, values);
        others.computeIfAbsent(title, k -> new ArrayList<>()).addAll(list);
        return this;
    }

    public String build() {
        StringBuilder builder = new StringBuilder();
        // 简介
        appendSection(builder, "简介", introduces);
        // 入参
        appendKvMap(builder, params);
        // 结果
        appendKvMap(builder, results);
        // 特殊
        appendSection(builder, "特殊说明", specials);
        // 其它
        others.forEach((title, contents) -> appendSection(builder, title, contents));
        return builder.toString().trim();
    }

    public Direction defaultIntroduce() {
        return addSuggestions(
                "LogPicker为一款自研的日志查询框架，支持嵌入式，或独立部署的方式运行",
                "可直接查询单台服务器，也可搭配“日志管理器”同时查询多台服务器"
        );
    }

    public Direction defaultParams() {
        return addParams(TITLE_PARAMS_STD, new LinkedHashMap<String, String>() {{
            put("fromTime", "开始时间，支持下方所列的多种时间格式(默认当天00:00)");
            put("toTime", "结束时间，支持下方所列的多种时间格式(默认当前时间)");
            put("countLimit", "一次查询最多返回的结果条数(默认使用配置值)");
            put("logFiles", "指定待查询的文件全路径，如“D:\\logs\\xxx.log”，多个时使用“;”或“,”进行分隔(默认根据时间选择文件)");
            put("uidList", "指定待查询的uid，多个时使用“;”或“,”进行分隔(默认不指定)");
            put("resultId", "查询指定id的结果，用于分页查询(默认不指定)");
        }}).addParams(TITLE_PARAMS_TAG, new LinkedHashMap<String, String>() {{
            put("tag-url", "基于url查询");
            put("tag-user", "基于user查询");
            put("tag-param", "基于param查询");
        }}).addParams(TITLE_PARAMS_FILTER, new LinkedHashMap<String, String>() {{
            put("filter-containsKey", "key包含，不区分大小写，支持同时指定多个");
        }});
    }

    public Direction defaultResults() {
        return addResults(TITLE_RESULTS_INFO, new LinkedHashMap<String, String>() {{
            put("msg", "框架内部处理过程中产生的一些信息，包含“使用的tag类型”、“文件索引范围”、“总耗时”、“查询耗时”、“结果数”等");
            put("endReason", "查询结束的原因，包含“已完成搜索”、“已找到指定数目的结果”、“已达搜索长度上限”等");
            put("curResultId", "当前的分页id，在请求参数中指定可找回该次搜索结果");
            put("lastResultId", "上一分页id，在请求参数中指定可找回上一分页的搜索结果");
            put("nextResultId", "下一分页id，在请求参数中指定可开始下一分页的搜索");
        }}).addResults(TITLE_RESULTS_ITEM, new LinkedHashMap<String, String>() {{
            put("time", "业务请求产生的时间，对应fromTime与toTime查询参数");
            put("spend", "业务请求的耗时");
            put("tags", "业务请求包含的标签，一般有url、user、param，对应tag-url、tag-user与tag-param");
            put("uid", "业务请求的唯一uid，对应uidList查询参数");
            put("thread", "业务日志产生时所在的线程");
            put("logs", "具体的业务日志列表");
        }});
    }

    public Direction defaultSpecials() {
        return addSpecials(
                "由于该框架使用的是“按需索引”的处理方案，也就是“有查询请求时，才会对一个日志文件进行索引”，所以首次调用查询接口的耗时会较长（包含索引耗时，速度大概20+M/s，对于一个3G的日志文件大概需要耗时150秒），需耐心等待。可参考返回结果上方的概要信息，了解索引耗时、查询耗时等信息",
                "索引建立后的请求，只要查询入参有指定至少一个索引tag，即可极大减少文件查找范围，获得毫秒级的搜索速度",
                "存在分页的情况下，下一页结果的时间可能比上一页的早：匹配时是按索引记录的顺序进行的，也就是a记录处理完后，再处理b记录（b的出现时间比a早，但索引顺序在a后）"
        );
    }

    public Direction defaultTimeSupports() {
        return addOthers("支持的时间格式",
                "yyyy-MM-dd HH:mm:ss(最完整)",
                "yyyy-MM-dd HH:mm(不含秒)",
                "yy-MM-dd HH:mm:ss(简写)",
                "yy-MM-dd HH:mm(简写，不含秒)",
                "HH:mm:ss(日期为今天)",
                "HH:mm(日期为今天，不含秒)"
        );
    }

    // ***********************内部方法****************************

    private void appendKv(List<String> list, Map<String, String> map) {
        map.forEach((k, v) -> list.add(k + ": " + v));
    }

    private void appendKvMap(StringBuilder builder, Map<String, Map<String, String>> map) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        map.forEach((title, items) -> appendKv(result.computeIfAbsent(title, k -> new ArrayList<>()), items));
        result.forEach((title, contents) -> appendSection(builder, title, contents));
    }

    private void appendSection(StringBuilder builder, String title, List<String> contents) {
        if (contents.isEmpty()) {
            return;
        }
        builder.append("* ").append(title).append("\n");
        for (String content : contents) {
            for (int i = 0; i < 2; i++) {
                builder.append(" ");
            }
            builder.append(content).append("\n");
        }
        builder.append("\n");
    }

}
