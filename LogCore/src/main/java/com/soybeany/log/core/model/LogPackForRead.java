package com.soybeany.log.core.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 供阅读的结果
 *
 * @author Soybeany
 * @date 2020/12/31
 */
public class LogPackForRead implements Serializable {

    /**
     * 日期
     */
    public String time;

    /**
     * 耗时，一般为具体值，但若缺失统计标签，则显示备注
     */
    public String spend;

    /**
     * 自定义标签，如url、user、param等
     */
    public final Map<String, String> tags = new LinkedHashMap<>();

    /**
     * 服务器
     */
    public String server;

    /**
     * 全局唯一号
     */
    public String uid;

    /**
     * 线程
     */
    public String thread;

    /**
     * 日志
     */
    public final List<String> logs = new LinkedList<>();

}
