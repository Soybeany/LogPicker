package com.soybeany.log.core.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class LogSection implements Serializable {

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
    public Map<String, String> tags;

    /**
     * 服务器
     */
    public String server;

    /**
     * 调用深度，每跨1个服务器，层级值加1
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
