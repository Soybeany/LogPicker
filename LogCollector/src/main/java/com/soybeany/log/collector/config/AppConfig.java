package com.soybeany.log.collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
@Component
@ConfigurationProperties(prefix = "config")
public class AppConfig {

    public String dirsToScan;
    public String logParseMode;
    public String lineParseRegex;
    public String lineTimeFormat;
    public int linesToBatchSave;
    public int maxResultCount;
    public int pageSizeCoefficientWithoutTag;
    public int maxPageSize;
    public int maxLinesPerResultWithNullUid;
    public int maxBytesReturn;

    public void setDirsToScan(String dirsToScan) {
        this.dirsToScan = dirsToScan;
    }

    public void setLogParseMode(String logParseMode) {
        this.logParseMode = logParseMode;
    }

    public void setLineParseRegex(String regex) {
        this.lineParseRegex = regex;
    }

    public void setLineTimeFormat(String lineTimeFormat) {
        this.lineTimeFormat = lineTimeFormat;
    }

    public void setLinesToBatchSave(int linesToBatchSave) {
        this.linesToBatchSave = linesToBatchSave;
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
