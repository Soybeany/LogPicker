package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.Context;
import com.soybeany.log.collector.model.LogSection;
import com.soybeany.log.collector.model.QueryParam;
import com.soybeany.log.collector.model.ResultVO;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.LogLineInfoRepository;
import com.soybeany.log.collector.service.filter.LogFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface QueryService {

    @NonNull
    List<ResultVO> query(Map<String, String> param);

}

@Service
class QueryServiceImpl implements QueryService {

    @Autowired
    private TagInfoService tagInfoService;
    @Autowired
    private List<LogFilter> logFilters;
    @Autowired
    private LogLineInfoRepository logLineInfoRepository;

    @Override
    public List<ResultVO> query(Map<String, String> param) {
        QueryParam queryParam = new QueryParam(param);
        // 若有指定标签，则先使用标签筛选
        if (tagInfoService.hasParams(queryParam)) {
            return getResultsWithTags(queryParam);
        }
        // 否则直接查记录表
        return getResultsWithoutTags(queryParam);
    }

    @PostConstruct
    private void onInit() {
        Collections.sort(logFilters);
    }

    @NonNull
    private List<ResultVO> getResultsWithTags(QueryParam queryParam) {
        // 使用标签过滤
        List<String> uidList = tagInfoService.getMatchedUidList(queryParam);
        if (uidList.isEmpty()) {
            return Collections.emptyList();
        }
        // 回表，查找记录
        List<ResultVO> result = new LinkedList<>();
        for (String uid : uidList) {
            List<LogLineInfo> logLineList = logLineInfoRepository.findByUid(uid);
            result.add(toResultVO(uid, logLineList));
        }
        // 使用过滤器过滤
        Context context = new Context(queryParam);
        for (ResultVO vo : result) {
            Iterator<LogSection> iterator = vo.sections.iterator();
            while (iterator.hasNext()) {
                LogSection section = iterator.next();
                for (LogFilter filter : logFilters) {
                    if (filter.shouldFilter(context, section)) {
                        iterator.remove();
                    }
                }
            }
        }
        return result;
    }

    @NonNull
    private List<ResultVO> getResultsWithoutTags(QueryParam queryParam) {
        return Collections.emptyList();
    }

    private ResultVO toResultVO(String uid, List<LogLineInfo> list) {
        ResultVO vo = new ResultVO();
        vo.uid = uid;
        vo.sections = new LinkedList<>();
        LogSection section = new LogSection();
        section.logs = new LinkedList<>();
        vo.sections.add(section);
        for (LogLineInfo info : list) {
            section.logs.add(info.fromByte + "~" + info.toByte);
        }
        return vo;
    }

}
