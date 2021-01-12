package com.soybeany.log.collector.controller;

import com.soybeany.log.collector.service.scan.ScanService;
import com.soybeany.log.core.model.LogException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Soybeany
 * @date 2021/1/8
 */
@RestController
@RequestMapping("/scan")
public class ScanController {

    @Autowired
    private ScanService scanService;

    @GetMapping("/full")
    public String full() {
        try {
            long start = System.currentTimeMillis();
            scanService.fullScan();
            long end = System.currentTimeMillis();
            String spend = "ok，耗时:" + ((end - start) / 1000) + "s";
            System.out.println(spend);
            return spend;
        } catch (LogException e) {
            return "异常:" + e.getMessage();
        }
    }

}
