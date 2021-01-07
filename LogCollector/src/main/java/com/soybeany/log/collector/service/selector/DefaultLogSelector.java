package com.soybeany.log.collector.service.selector;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.QueryParam;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.LogLineInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
@Component
class DefaultLogSelector implements LogSelector {

    @Autowired
    private LogLineInfoRepository logLineInfoRepository;

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean isSupport(QueryContext context) {
        return true;
    }

    @Override
    public List<LogLineInfo> select(QueryContext context, int page, int pageSize) {
        QueryParam queryParam = context.queryParam;
        Pageable pageable = PageRequest.of(page, pageSize);
        return logLineInfoRepository.findByTimeBetweenOrderByTime(queryParam.getFromTime(), queryParam.getToTime(), pageable);
    }
}
