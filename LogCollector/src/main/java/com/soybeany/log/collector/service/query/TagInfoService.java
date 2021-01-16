package com.soybeany.log.collector.service.query;

import com.soybeany.log.collector.repository.LogTagInfo;
import com.soybeany.log.collector.repository.LogTagInfoRepository;
import com.soybeany.log.collector.service.query.model.QueryContext;
import com.soybeany.log.collector.service.query.model.QueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
public interface TagInfoService {

    boolean hasTags(QueryContext context);

    @NonNull
    List<String> getMatchedUidList(QueryContext context, int page, int pageSize);

}

@Service
class TagInfoServiceImpl implements TagInfoService {

    private static final String PREFIX = "tag";

    @Autowired
    private LogTagInfoRepository logTagInfoRepository;

    @Override
    public boolean hasTags(QueryContext context) {
        return !context.queryParam.getParams(PREFIX).isEmpty();
    }

    @Override
    public List<String> getMatchedUidList(QueryContext context, int page, int pageSize) {
        // 提取参数
        QueryParam queryParam = context.queryParam;
        Map<String, String> params = queryParam.getParams(PREFIX);
        if (params.isEmpty()) {
            return Collections.emptyList();
        }
        Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
        Map.Entry<String, String> firstEntry = iterator.next();
        Pageable pageable = PageRequest.of(page, pageSize);
        // 首次筛选
        List<LogTagInfo> infoList = logTagInfoRepository.findByKeyAndTimeBetweenAndValueContainingOrderByTime(firstEntry.getKey(), queryParam.getFromTime(), queryParam.getToTime(), firstEntry.getValue(), pageable);
        // 进阶循环筛选
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            List<String> uids = toUidList(infoList);
            if (uids.isEmpty()) {
                return Collections.emptyList();
            }
            infoList = logTagInfoRepository.findByKeyAndValueContainingAndUidInOrderByTime(entry.getKey(), entry.getValue(), uids);
        }
        // 转换为uid列表
        return toUidList(infoList);
    }

    @NonNull
    private List<String> toUidList(List<LogTagInfo> list) {
        List<String> result = new LinkedList<>();
        for (LogTagInfo info : list) {
            result.add(info.uid);
        }
        return result;
    }

}
