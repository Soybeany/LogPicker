package com.soybeany.log.collector.vo;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 一条匹配的结果
 *
 * @author Soybeany
 * @date 2020/12/31
 */
public class ResultVO implements Serializable {

    /*
     todo 数据库存储
     flags一张表，保存各种flag->uui
     */


    /**
     * 全局唯一号
     */
    public String uuid;

    /**
     * 日志分段，即不同来源的各段
     */
    public List<LogSection> sections;

    private static class LogSection implements Serializable {

        /**
         * 开始时间，若有开始标签则为标签时间，否则为第一条日志的时间
         */
        public String startTime;

        /**
         * 耗时，一般为具体值，但若缺失统计标签，则会显示备注
         */
        public String spend;

        /**
         * 自定义标签，如url、user、param等
         */
        public Map<String, String> flags;

        /**
         * 服务器
         */
        public String server;

        /**
         * 调用层级，每跨1个服务器，层级值加1
         */
        public int depth;

        /**
         * 线程
         */
        public String thread;

        /**
         * 日志
         */
        public List<String> logs;

    }

    public static void main(String[] args) {
        ResultVO resultVO = new ResultVO();
        resultVO.uuid = "12345";
        resultVO.sections = new LinkedList<>();

        LogSection section1 = new LogSection();
        section1.server = "server1";
        section1.depth = 0;
        section1.thread = "thread1";
        section1.startTime = "2020-12-31 16:31:00";
        section1.spend = "2s";
        section1.flags = new LinkedHashMap<>();
        section1.flags.put("url", "/test.do");
        section1.flags.put("user", "238744");
        section1.logs = new LinkedList<>();
        section1.logs.add("这是第一行log");
        section1.logs.add("这是第二行log");

        LogSection section2 = new LogSection();
        section2.server = "server1";
        section2.depth = 0;
        section2.thread = "thread1";
        section2.startTime = "2020-12-31 16:31:00";
        section2.spend = "2s";
        section2.flags = new LinkedHashMap<>();
        section2.flags.put("url", "/test.do");
        section2.flags.put("user", "238744");
        section2.logs = new LinkedList<>();
        section2.logs.add("这是第一行log");
        section2.logs.add("这是第二行log");

        resultVO.sections.add(section1);
        resultVO.sections.add(section2);

        Gson gson = new Gson();
        System.out.println(gson.toJson(resultVO));
    }

}
