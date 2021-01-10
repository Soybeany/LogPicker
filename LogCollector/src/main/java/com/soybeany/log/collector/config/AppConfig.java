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

    public int maxPageSize;
    public int maxLinesPerResultWithNullUid;
    public int maxBytesReturn;
    public String dirToScan;

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public void setMaxLinesPerResultWithNullUid(int maxLinesPerResultWithNullUid) {
        this.maxLinesPerResultWithNullUid = maxLinesPerResultWithNullUid;
    }

    public void setMaxBytesReturn(int maxBytesReturn) {
        this.maxBytesReturn = maxBytesReturn;
    }

    public void setDirToScan(String dirToScan) {
        this.dirToScan = dirToScan;
    }
}
