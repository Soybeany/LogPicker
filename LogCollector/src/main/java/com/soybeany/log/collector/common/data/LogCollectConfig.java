package com.soybeany.log.collector.common.data;

import com.soybeany.log.core.util.Md5Calculator;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public class LogCollectConfig {

    /**
     * 待扫描的目录，多个使用“;”进行分隔
     */
    public String[] dirsToScan;
    /**
     * 当天日志文件的命名
     */
    public String logTodayFileName;
    /**
     * 历史日志文件的命名，使用<?TimeFormat?>作时间占位
     */
    public String logHistoryFileName;
    /**
     * 日志的字符集
     */
    public String logCharset = "GBK";
    /**
     * 行解析的正则
     */
    public Pattern lineParsePattern;
    /**
     * 标签解析的正则
     */
    public Pattern tagParsePattern;
    /**
     * 没有uid时，使用的占位符
     */
    public String noUidPlaceholder = "";
    /**
     * 行的时间格式(字符串)
     */
    public String lineTimeFormat;
    /**
     * 行的时间格式(对象)
     */
    public DateTimeFormatter lineTimeFormatter;
    /**
     * 需要建立索引的标签，多个使用“;”进行分隔
     */
    public final Set<String> tagsToIndex = new LinkedHashSet<>();
    /**
     * 当新记录与旧记录间不超过指定字节数时，将新记录合并至旧记录
     */
    public int maxBytesGapToMerge = 10000;
    /**
     * 当日志行没有uid时，每条查询结果允许包含的最大行数
     */
    public int maxLinesPerResultWithNoUid = 20;
    /**
     * 默认一次查询，最多返回的结果条数
     */
    public int defaultMaxResultCount = 30;
    /**
     * 一次查询最多能查询的文件数
     */
    public int maxFilesToQuery = 10;
    /**
     * 在内存中最多持有的文件索引数
     */
    public int maxFileIndexesRetain = 10;
    /**
     * 在内存中最多持有的结果数
     */
    public int maxResultRetain = 10;
    /**
     * 索引保留的时间
     */
    public int indexRetainSec = 300;
    /**
     * 查询结果保留的时间
     */
    public int resultRetainSec = 300;

    public LogCollectConfig(String[] dirsToScan, String logTodayFileName,
                            String logHistoryFileName, String lineParseRegex, String tagParseRegex,
                            String lineTimeFormat) {
        this.dirsToScan = dirsToScan;
        this.logTodayFileName = logTodayFileName;
        this.logHistoryFileName = logHistoryFileName;
        this.lineParsePattern = Pattern.compile(lineParseRegex);
        this.tagParsePattern = Pattern.compile(tagParseRegex);
        this.lineTimeFormat = lineTimeFormat;
        this.lineTimeFormatter = DateTimeFormatter.ofPattern(lineTimeFormat);
    }

    public LogCollectConfig withLogCharset(String logCharset) {
        this.logCharset = logCharset;
        return this;
    }

    public LogCollectConfig withNoUidPlaceholder(String noUidPlaceholder) {
        this.noUidPlaceholder = noUidPlaceholder;
        return this;
    }

    public LogCollectConfig withTagsToIndex(Collection<String> tagsToIndex) {
        this.tagsToIndex.addAll(tagsToIndex);
        return this;
    }

    public LogCollectConfig withMaxBytesGapToMerge(int maxBytesGapToMerge) {
        this.maxBytesGapToMerge = maxBytesGapToMerge;
        return this;
    }

    public LogCollectConfig withMaxLinesPerResultWithNoUid(int maxLinesPerResultWithNoUid) {
        this.maxLinesPerResultWithNoUid = maxLinesPerResultWithNoUid;
        return this;
    }

    public LogCollectConfig withDefaultMaxResultCount(int defaultMaxResultCount) {
        this.defaultMaxResultCount = defaultMaxResultCount;
        return this;
    }

    public LogCollectConfig withMaxFileIndexesRetain(int count) {
        this.maxFileIndexesRetain = count;
        return this;
    }

    public LogCollectConfig withMaxResultRetain(int count) {
        this.maxResultRetain = count;
        return this;
    }

    public LogCollectConfig withMaxFilesToQuery(int count) {
        this.maxFilesToQuery = count;
        return this;
    }

    public LogCollectConfig withIndexRetainSec(int sec) {
        this.indexRetainSec = sec;
        return this;
    }

    public LogCollectConfig withResultRetainSec(int resultRetainSec) {
        this.resultRetainSec = resultRetainSec;
        return this;
    }

    public String getConfigMd5() throws Exception {
        return new Md5Calculator()
                .with(logCharset).with(lineParsePattern).with(tagParsePattern).with(noUidPlaceholder)
                .with(lineTimeFormat).with(lineTimeFormatter).with(tagsToIndex)
                .calculate();
    }

}
