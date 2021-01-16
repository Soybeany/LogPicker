package com.soybeany.log.collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
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
    public String logParseMode;
    public String logCharset;
    public Pattern lineParsePattern;
    public String lineTimeFormat;
    public DateTimeFormatter lineTimeFormatter;
    public final Set<String> tagsToIndex = new HashSet<>();
    public int maxLinesGapToMerge;
    public int maxResultCount;
    public int pageSizeCoefficientWithoutTag;
    public int maxPageSize;
    public int maxLinesPerResultWithNullUid;
    public int maxBytesReturn;

    public void setDirsToScan(String dirsToScan) {
        this.dirsToScan = dirsToScan.split(";");
    }

    public void setDirForIndexes(String dirForIndexes) {
        this.dirForIndexes = dirForIndexes;
    }

    public void setLogParseMode(String logParseMode) {
        this.logParseMode = logParseMode;
    }

    public void setLogCharset(String logCharset) {
        this.logCharset = logCharset;
    }

    public void setLineParseRegex(String regex) {
        this.lineParsePattern = Pattern.compile(regex);
    }

    public void setLineTimeFormat(String lineTimeFormat) {
        this.lineTimeFormat = lineTimeFormat;
        this.lineTimeFormatter = DateTimeFormatter.ofPattern(lineTimeFormat);
    }

    public void setTagsToIndex(String tagsToIndex) {
        this.tagsToIndex.addAll(Arrays.asList(tagsToIndex.split(";")));
    }

    public void setMaxLinesGapToMerge(int maxLinesGapToMerge) {
        this.maxLinesGapToMerge = maxLinesGapToMerge;
    }

    public void setMaxResultCount(int maxResultCount) {
        this.maxResultCount = maxResultCount;
    }

    public void setPageSizeCoefficientWithoutTag(int pageSizeCoefficientWithoutTag) {
        this.pageSizeCoefficientWithoutTag = pageSizeCoefficientWithoutTag;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public void setMaxLinesPerResultWithNullUid(int maxLinesPerResultWithNullUid) {
        this.maxLinesPerResultWithNullUid = maxLinesPerResultWithNullUid;
    }

    public void setMaxBytesReturn(int maxBytesReturn) {
        this.maxBytesReturn = maxBytesReturn;
    }

}
