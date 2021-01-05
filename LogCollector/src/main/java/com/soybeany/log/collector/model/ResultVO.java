package com.soybeany.log.collector.model;

import java.io.Serializable;
import java.util.List;

/**
 * 一条匹配的结果
 *
 * @author Soybeany
 * @date 2020/12/31
 */
public class ResultVO implements Serializable {

    /**
     * 全局唯一号
     */
    public String uid;

    /**
     * 日志分段，即不同来源的各段
     */
    public List<LogSection> sections;

}
