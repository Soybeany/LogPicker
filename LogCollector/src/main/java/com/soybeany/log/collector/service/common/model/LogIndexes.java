package com.soybeany.log.collector.service.common.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Soybeany
 * @date 2021/1/14
 */
public class LogIndexes implements Serializable {

    /**
     * 被索引文件的路径
     */
    public String filePath;

    /**
     * 已扫描的字节数
     */
    public long scannedBytes;

    /**
     * 时间索引(开始时间使用匹配的值；结束时间使用匹配的下一值，若无则使用文件大小值)
     */
    public final TreeMap<String, Long> timeIndexMap = new TreeMap<>();

    /**
     * 自定义标签索引
     */
    public final Map<String, Map<String, List<FileRange>>> tagsIndexMap = new HashMap<>();

}
