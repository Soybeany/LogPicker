package com.soybeany.log.core.model;

import java.io.Serializable;

/**
 * @author Soybeany
 * @date 2021/1/15
 */
public class FileRange implements Serializable {

    public static final FileRange EMPTY = new FileRange(0, 0);

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
