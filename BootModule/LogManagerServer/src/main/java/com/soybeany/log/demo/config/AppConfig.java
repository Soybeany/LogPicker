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

    public void setQueryPath(String queryPath) {
        this.queryPath = queryPath;
    }
}
