package com.soybeany.log.collector.config;

import com.soybeany.log.core.util.Md5Calculator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
@Component
@ConfigurationProperties(prefix = "config")
public class AppConfig {

    public String[] dirsToScan;
    public String dirForIndexes;
    public String logTodayFileName;
    public String logHistoryFileName;
    public String logCharset;
    public Pattern lineParsePattern;
    public Pattern tagParsePattern;
    public String noUidPlaceholder;
    public String lineTimeFormat;
    public DateTimeFormatter lineTimeFormatter;
    public final Set<String> tagsToIndex = new LinkedHashSet<>();
    public int maxBytesGapToMerge;
    public int maxLinesPerResultWithNoUid;
    public int defaultMaxResultCount;

    public void setDirsToScan(String dirsToScan) {
        this.dirsToScan = dirsToScan.split(";");
    }

    public void setDirForIndexes(String dirForIndexes) {
        this.dirForIndexes = dirForIndexes;
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
        this.lineParsePattern = Pattern.compile(regex);
    }

    public void setTagParseRegex(String regex) {
        this.tagParsePattern = Pattern.compile(regex);
    }

    public void setNoUidPlaceholder(String noUidPlaceholder) {
        this.noUidPlaceholder = noUidPlaceholder;
    }

    public void setLineTimeFormat(String lineTimeFormat) {
        this.lineTimeFormat = lineTimeFormat;
        this.lineTimeFormatter = DateTimeFormatter.ofPattern(lineTimeFormat);
    }

    public void setTagsToIndex(String tagsToIndex) {
        this.tagsToIndex.addAll(Arrays.asList(tagsToIndex.split(";")));
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

    public String getConfigMd5() throws Exception {
        return new Md5Calculator()
                .with(logCharset).with(lineParsePattern).with(tagParsePattern).with(noUidPlaceholder)
                .with(lineTimeFormat).with(lineTimeFormatter).with(tagsToIndex)
                .calculate();
    }

}
