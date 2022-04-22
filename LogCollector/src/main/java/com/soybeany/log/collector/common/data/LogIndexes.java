package com.soybeany.log.collector.common.data;

import com.soybeany.log.core.model.FileRange;

import java.io.File;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/14
 */
public class LogIndexes extends BaseUnit {

    /**
     * 使用uid作为key，存放该uid出现的模糊范围(包含但不全是)
     */
    public final Map<String, LinkedList<FileRange>> uidRanges = new HashMap<>();

    /**
     * 时间下标索引(开始时间使用匹配的值；结束时间使用匹配的下一值，若无则使用文件大小值)
     */
    public final TreeMap<Long, Long> timeIndexMap = new TreeMap<>();

    /**
     * 自定义标签索引，第一个key为tagName，第二个key为tagValue，value为uid集合
     */
    public final Map<String, Map<String, Set<String>>> tagUidMap = new HashMap<>();

    // ********************普通方法区********************

    public LogIndexes(LogCollectConfig logCollectConfig, File logFile) {
        super(logCollectConfig, logFile);
    }

}
