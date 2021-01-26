package com.soybeany.log.collector.service.common.data;

import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

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
     * 使用uid作为key
     */
    public final Map<String, LogPack> uidMap = new HashMap<>();

    /**
     * 时间索引(开始时间使用匹配的值；结束时间使用匹配的下一值，若无则使用文件大小值)
     */
    public final TreeMap<String, Long> timeIndexMap = new TreeMap<>();

    /**
     * 自定义标签索引
     */
    public final Map<String, Map<String, LinkedList<FileRange>>> tagsIndexMap = new HashMap<>();

    public LogIndexes(File logFile) {
        this.logFile = logFile;
    }

    // ********************内部类********************

    public interface Updater {
        LogIndexes updateAndGet(File logFile);
    }

}
