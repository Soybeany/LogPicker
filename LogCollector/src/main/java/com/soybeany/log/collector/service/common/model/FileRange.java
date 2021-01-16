package com.soybeany.log.collector.service.common.model;

/**
 * @author Soybeany
 * @date 2021/1/15
 */
public class FileRange {

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
