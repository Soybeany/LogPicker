package com.soybeany.log.core.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * 裁切后的日志包
 *
 * @author Soybeany
 * @date 2021/1/7
 */
public class LogPack implements Serializable {

    /**
     * 开始标签
     */
    public LogTag startTag;

    /**
     * 结束标签
     */
    public LogTag endTag;

    /**
     * 自定义标签，如url、user、param等
     */
    public final List<LogTag> tags = new LinkedList<>();

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
    public final List<LogLine> logLines = new LinkedList<>();

    /**
     * 在文件中出现的范围
     */
    public final List<FileRange> ranges = new LinkedList<>();

}
