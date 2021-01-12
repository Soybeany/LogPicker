package com.soybeany.log.collector.controller;

import com.soybeany.log.collector.service.scan.ScanService;
import com.soybeany.log.core.model.LogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(Object.class);

    @Autowired
    private ScanService scanService;

    @GetMapping("/full")
    public String full() {
        try {
            LOG.info("开始扫描");
            long start = System.currentTimeMillis();
            scanService.fullScan();
            long end = System.currentTimeMillis();
            String spend = "ok，耗时:" + ((end - start) / 1000) + "s";
            LOG.info(spend);
            return spend;
        } catch (LogException e) {
            return "异常:" + e.getMessage();
        }
    }

}
