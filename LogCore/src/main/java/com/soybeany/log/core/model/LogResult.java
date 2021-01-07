package com.soybeany.log.core.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 一条匹配的结果，包含来自各台服务器的分段日志
 *
 * @author Soybeany
 * @date 2020/12/31
 */
public class LogResult implements Serializable {

    /**
     * 时间，默认为开始时间；也可能是结束时间(添加标识)；也可能是未知(2个标签均缺失)
     */
    public String time;

    /**
     * 耗时，一般为具体值，但若缺失统计标签，则显示备注
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
    public List<String> logs;

}
