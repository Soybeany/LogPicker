package com.soybeany.log.demo.controller;

import com.soybeany.log.demo.config.AppConfig;
import com.soybeany.log.manager.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/2/5
 */
@RestController
@RequestMapping(ManagerController.PATH_PREFIX)
public class ManagerController {

    static final String PATH_PREFIX = "/api/v1/logMonitorV4";

    @Autowired
    private AppConfig appConfig;

    @PostMapping(value = "/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public String forDirectRead(@RequestParam Map<String, String> param) {
        return LogManager.query().getResult(appConfig.queryPath, param, appConfig.resultRetainSec);
    }

    @GetMapping("/help")
    public String help() {
        return LogManager.queryHelp(PATH_PREFIX + "/help", PATH_PREFIX + "/query");
    }

}
