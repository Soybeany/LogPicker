package com.soybeany.log.demo.config;

import com.soybeany.log.manager.LogManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author Soybeany
 * @date 2021/2/10
 */
@Component
@ConfigurationProperties(prefix = "config")
public class AppConfig {

    public String queryPath;
    public int resultRetainSec;

    @PostConstruct
    void onInit() {
        LogManager.init();
    }

    @PreDestroy
    void onDestroy() {
        LogManager.release();
    }

    public void setQueryPath(String queryPath) {
        this.queryPath = queryPath;
    }

    public void setResultRetainSec(int resultRetainSec) {
        this.resultRetainSec = resultRetainSec;
    }
}
