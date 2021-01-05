package com.soybeany.log.collector.controller;

import com.soybeany.log.collector.model.QueryParam;
import com.soybeany.log.collector.service.TagInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
@RestController
@RequestMapping("/test1")
public class TestController {

    @Autowired
    private TagInfoService tagInfoService;

    @GetMapping("/test")
    public String test(@RequestParam Map<String, String> param) {
        List<String> uidList = tagInfoService.getMatchedUidList(new QueryParam(param));
        return "匹配到:" + uidList.size();
    }

}
