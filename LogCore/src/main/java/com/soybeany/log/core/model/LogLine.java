package com.soybeany.log.core.model;

import java.io.Serializable;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public class LogLine implements Serializable {

    /**
     * 时间
     */
    public String time;

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
