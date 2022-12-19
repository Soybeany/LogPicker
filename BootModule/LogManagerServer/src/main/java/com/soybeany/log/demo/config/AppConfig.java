package com.soybeany.log.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Soybeany
 * @date 2021/2/10
 */
@Component
@ConfigurationProperties(prefix = "config")
public class AppConfig {

    public String queryPath;
    public String helpPath;
    public int maxResultRetain;
    public int resultRetainSec;

    public void setQueryPath(String queryPath) {
        this.queryPath = queryPath;
    }

    public void setHelpPath(String helpPath) {
        this.helpPath = helpPath;
    }

    public void setMaxResultRetain(int maxResultRetain) {
        this.maxResultRetain = maxResultRetain;
    }

    public void setResultRetainSec(int resultRetainSec) {
        this.resultRetainSec = resultRetainSec;
    }
}
