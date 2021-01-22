package com.soybeany.log.collector.service.common.model;

import java.io.Serializable;

/**
 * @author Soybeany
 * @date 2021/1/15
 */
public class FileRange implements Serializable {

    /**
     * 开始位置，包含
     */
    public long from;

    /**
     * 结束位置，不包含
     */
    public long to;

    public FileRange(long from, long to) {
        this.from = from;
        this.to = to;
    }
}
