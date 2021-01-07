package com.soybeany.log.collector.model;

import org.springframework.lang.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public class RawLogResult {

    /**
     * 自定义标签，如url、user、param等
     */
    @Nullable
    public Map<String, String> tags;

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

}
