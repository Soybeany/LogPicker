package com.soybeany.log.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public class LogTag implements Serializable {

    /**
     * 所属的uid
     */
    public String uid;

    /**
     * 线程
     */
    public String thread;

    /**
     * 创建该标记的时间
     */
    public Date time;

    /**
     * 标签的键
     */
    public String key;

    /**
     * 标签的值
     */
    public String value;

}
