package com.soybeany.log.collector.service.common.data;

import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/14
 */
public class LogIndexes implements Serializable {

    /**
     * 日志文件
     */
    public final File logFile;

    /**
     * 已扫描的字节数
     */
    public long scannedBytes;

    /**
     * 使用uid作为key，存放未组装完成的临时记录
     */
    public final Map<String, LogPack> uidTempMap = new HashMap<>();

    /**
     * 使用uid作为key，存放该uid出现的模糊范围(包含但不全是)
     */
    public final Map<String, LinkedList<FileRange>> uidRanges = new HashMap<>();

    /**
     * 时间下标索引(开始时间使用匹配的值；结束时间使用匹配的下一值，若无则使用文件大小值)
     */
    public final TreeMap<String, Long> timeIndexMap = new TreeMap<>();

    /**
     * 自定义标签索引，第一个key为tagName，第二个key为tagValue，value为uid集合
     */
    public final Map<String, Map<String, Set<String>>> tagUidMap = new HashMap<>();

    public LogIndexes(File logFile) {
        this.logFile = logFile;
    }

    // ********************内部类********************

    public interface Updater {
        LogIndexes updateAndGet(File logFile);
    }

}
