package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.QueryParam;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.LogLineInfoRepository;
import com.soybeany.log.core.model.LogLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
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
    @NonNull
    List<LogLine> select(QueryContext context, int page, int pageSize);

}

@Service
class LogSelectorServiceImpl implements LogSelectorService {

    @Autowired
    private TagInfoService tagInfoService;
    @Autowired
    private LogLineConvertService logLineConvertService;
    @Autowired
    private LogLineInfoRepository logLineInfoRepository;

    @Override
    public List<LogLine> select(QueryContext context, int page, int pageSize) {
        List<LogLineInfo> list;
        if (tagInfoService.hasTags(context)) {
            list = selectWithTag(context, page, pageSize);
        } else {
            list = selectWithoutTag(context, page, pageSize);
        }
        return logLineConvertService.convert(context, list);
    }

    @NonNull
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

    @NonNull
    private List<LogLineInfo> selectWithoutTag(QueryContext context, int page, int pageSize) {
        QueryParam queryParam = context.queryParam;
        Pageable pageable = PageRequest.of(page, pageSize);
        return logLineInfoRepository.findByTimeBetweenOrderByTime(queryParam.getFromTime(), queryParam.getToTime(), pageable);
    }
}
