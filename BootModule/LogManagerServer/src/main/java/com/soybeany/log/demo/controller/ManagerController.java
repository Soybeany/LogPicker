package com.soybeany.log.demo.controller;

import com.soybeany.log.demo.config.AppConfig;
import com.soybeany.log.manager.BaseExecutor;
import com.soybeany.log.manager.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/2/5
 */
@RestController
@RequestMapping("/query")
public class ManagerController {

    @Autowired
    private AppConfig appConfig;

    @PostMapping(value = "/forDirectRead", produces = MediaType.APPLICATION_JSON_VALUE)
    public String forDirectRead(@RequestParam Map<String, String> param) {
        return LogManager.query().getResult(appConfig.queryPath, param);
    }

    @GetMapping("/help")
    public String help() {
        return LogManager.queryHelp("/query/help", "/query/forDirectRead");
    }

    @PostConstruct
    void onInit() {
        BaseExecutor.createRequestPool();
    }

    @PreDestroy
    void onDestroy() {
        BaseExecutor.destroyRequestPool();
    }

}
