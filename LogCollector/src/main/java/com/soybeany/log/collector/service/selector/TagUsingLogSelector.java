package com.soybeany.log.collector.service.selector;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.LogLineInfoRepository;
import com.soybeany.log.collector.service.TagInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
@Component
class TagUsingLogSelector implements LogSelector {

    @Autowired
    private TagInfoService tagInfoService;
    @Autowired
    private LogLineInfoRepository logLineInfoRepository;

    @Override
    public boolean isSupport(QueryContext context) {
        return tagInfoService.hasParams(context);
    }

    @Override
    public List<LogLineInfo> select(QueryContext context, int page, int pageSize) {
        // 使用标签过滤
        List<String> uidList = tagInfoService.getMatchedUidList(context, page, pageSize);
        if (uidList.isEmpty()) {
            return Collections.emptyList();
        }
        // 回表，查找记录
        List<LogLineInfo> result = new LinkedList<>();
        for (String uid : uidList) {
            result.addAll(logLineInfoRepository.findByUidOrderByTime(uid));
        }
        return result;
    }
}
