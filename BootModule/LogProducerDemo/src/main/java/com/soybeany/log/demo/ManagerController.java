package com.soybeany.log.demo;

import com.soybeany.log.demo.manager.BaseExecutor;
import com.soybeany.log.demo.manager.LogManager;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/2/5
 */
@RestController
@RequestMapping("/query")
public class ManagerController {

    @PostMapping("/forDirectRead")
    public List<Object> forDirectRead(@RequestParam Map<String, String> param) {
        return LogManager.query().getResult(param);
    }

    @GetMapping("/help")
    public String help() {
        return LogManager.help();
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
