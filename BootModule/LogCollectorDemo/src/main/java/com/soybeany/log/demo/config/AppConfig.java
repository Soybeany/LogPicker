package com.soybeany.log.demo.config;

import com.soybeany.log.collector.common.data.LogCollectConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
@Component
@ConfigurationProperties(prefix = "config")
public class AppConfig {

    public String dirToScan;
    public String logTodayFileName;
    public String logHistoryFileName;
    public String logCharset;
    public String lineParseRegex;
    public String tagParseRegex;
    public String noUidPlaceholder;
    public String lineTimeFormat;
    public List<String> tagsToIndex = new LinkedList<>();
    public int maxBytesGapToMerge;
    public int maxLinesPerResultWithNoUid;
    public int defaultMaxResultCount;
    public int resultRetainSec;

    // ********************设置方法********************

    public void setDirToScan(String dirToScan) {
        this.dirToScan = dirToScan;
    }

    public void setLogTodayFileName(String logTodayFileName) {
        this.logTodayFileName = logTodayFileName;
    }

    public void setLogHistoryFileName(String logHistoryFileName) {
        this.logHistoryFileName = logHistoryFileName;
    }

    public void setLogCharset(String logCharset) {
        this.logCharset = logCharset;
    }

    public void setLineParseRegex(String regex) {
        this.lineParseRegex = regex;
    }

    public void setTagParseRegex(String regex) {
        this.tagParseRegex = regex;
    }

    public void setNoUidPlaceholder(String noUidPlaceholder) {
        this.noUidPlaceholder = noUidPlaceholder;
    }

    public void setLineTimeFormat(String lineTimeFormat) {
        this.lineTimeFormat = lineTimeFormat;
    }

    public void setTagsToIndex(String tagsToIndex) {
        Collections.addAll(this.tagsToIndex, tagsToIndex.split(";"));
    }

    public void setMaxBytesGapToMerge(int maxBytesGapToMerge) {
        this.maxBytesGapToMerge = maxBytesGapToMerge;
    }

    public void setDefaultMaxResultCount(int defaultMaxResultCount) {
        this.defaultMaxResultCount = defaultMaxResultCount;
    }

    public void setMaxLinesPerResultWithNoUid(int maxLinesPerResultWithNoUid) {
        this.maxLinesPerResultWithNoUid = maxLinesPerResultWithNoUid;
    }

    public void setResultRetainSec(int resultRetainSec) {
        this.resultRetainSec = resultRetainSec;
    }

    // ********************自定义方法********************

    public LogCollectConfig toLogCollectConfig() {
        return new LogCollectConfig(lineParseRegex, tagParseRegex, lineTimeFormat)
                .withLogCharset(logCharset)
                .withNoUidPlaceholder(noUidPlaceholder)
                .withTagsToIndex(tagsToIndex)
                .withMaxBytesGapToMerge(maxBytesGapToMerge)
                .withMaxLinesPerResultWithNoUid(maxLinesPerResultWithNoUid)
                .withDefaultMaxResultCount(defaultMaxResultCount)
                .withResultRetainSec(resultRetainSec);
    }

}
