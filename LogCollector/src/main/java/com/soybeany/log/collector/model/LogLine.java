package com.soybeany.log.collector.model;

import java.util.Date;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public class LogLine {

    /**
     * 时间
     */
    public Date time;

    /**
     * 全局唯一号
     */
    public String uid;

    /**
     * 线程
     */
    public String thread;

    /**
     * 等级
     */
    public String level;

    /**
     * 业务日志内容
     */
    public String content;

}
