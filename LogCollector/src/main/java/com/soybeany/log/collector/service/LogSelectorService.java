package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.QueryParam;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.LogLineInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/8
 */
public interface LogSelectorService {

    /**
     * 选择出合适的日志行
     *
     * @return 返回null或空列表，表示没有更多记录
     */
    @Nullable
    List<LogLineInfo> select(QueryContext context, int page, int pageSize);

}

@Service
class LogSelectorServiceImpl implements LogSelectorService {

    @Autowired
    private TagInfoService tagInfoService;
    @Autowired
    private LogLineInfoRepository logLineInfoRepository;

    @Override
    public List<LogLineInfo> select(QueryContext context, int page, int pageSize) {
        if (tagInfoService.hasParams(context)) {
            return selectWithTag(context, page, pageSize);
        }
        return selectWithoutTag(context, page, pageSize);
    }

    private List<LogLineInfo> selectWithTag(QueryContext context, int page, int pageSize) {
        // 使用标签查找匹配的uid
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

    private List<LogLineInfo> selectWithoutTag(QueryContext context, int page, int pageSize) {
        QueryParam queryParam = context.queryParam;
        Pageable pageable = PageRequest.of(page, pageSize);
        return logLineInfoRepository.findByTimeBetweenOrderByTime(queryParam.getFromTime(), queryParam.getToTime(), pageable);
    }
}
