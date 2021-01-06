package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.QueryParam;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.LogLineInfoRepository;
import com.soybeany.log.collector.service.filter.LogFilter;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogResult;
import com.soybeany.log.core.model.LogSection;
import com.soybeany.log.core.model.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface QueryService {

    @NonNull
    ResultVO query(Map<String, String> param);

}

@Service
class QueryServiceImpl implements QueryService, QueryContext.NextContextHandler {

    private static final String QUERY_PREFIX = "query";
    private static final String P_KEY_CONTEXT_ID = "contextId"; // 关联context的id，string
    private static final String C_KEY_TAG_START_PAGE = "tagStartPage"; // tag的开始分页下标(包含)，int
    private static final String C_KEY_TAG_END_PAGE = "tagEndPage"; // tag的结束分页下标(不包含)，int
    private static final String C_KEY_EX_RESULT = "exResult"; // 额外的结果，即上一次查询超出数目限制的结果，List<ResultVO>
    private static final String C_KEY_REST_RESULT = "restResult"; // 剩余的结果，即本次查询剩余的结果，List<ResultVO>

    @Autowired
    private TagInfoService tagInfoService;
    @Autowired
    private List<LogFilter> logFilters;
    @Autowired
    private LogLineInfoRepository logLineInfoRepository;

    private final Map<String, QueryContext> contextMap = new ConcurrentHashMap<>();

    @Override
    public void onHandleNextContext(QueryContext old, QueryContext next) {
        // 标签分页
        Integer tagEndPage = old.getData(QUERY_PREFIX, C_KEY_TAG_END_PAGE);
        next.putData(QUERY_PREFIX, C_KEY_TAG_START_PAGE, tagEndPage);
        // 查询结果
        List<ResultVO> restResult = old.getData(QUERY_PREFIX, C_KEY_REST_RESULT);
        next.putData(QUERY_PREFIX, C_KEY_EX_RESULT, restResult);
    }

    @Override
    public ResultVO query(Map<String, String> param) {
        QueryContext context = getQueryContext(param);
        try {
            // 获取锁
            context.lock.lock();
            // 如果context中已包含结果，则直接返回
            if (null != context.result) {
                return context.result;
            }
            // 若有指定标签，则先使用标签筛选
            int page = getPage(context);
            if (tagInfoService.hasParams(context)) {
                context.result = getResultsWithTags(context, page);
            }
            // 否则直接查记录表
            else {
                context.result = getResultsWithoutTags(context, page);
            }

            return context.result;
        } finally {
            context.clearTempData();
            // 释放锁
            context.lock.unlock();
        }
    }

    @PostConstruct
    private void onInit() {
        Collections.sort(logFilters);
    }

    private int getPage(QueryContext context) {
        Integer page = context.getData(QUERY_PREFIX, C_KEY_TAG_START_PAGE);
        if (null == page) {
            context.putData(QUERY_PREFIX, C_KEY_TAG_START_PAGE, page = 0);
        }
        return page;
    }

    private QueryContext getQueryContext(Map<String, String> param) {
        String contextId = QueryParam.getParam(QUERY_PREFIX, P_KEY_CONTEXT_ID, param);
        // 若指定了contextId，则尝试复用context
        if (null != contextId) {
            if (!contextMap.containsKey(contextId)) {
                throw new LogException("找不到指定contextId对应的context");
            }
            return contextMap.get(contextId);
        }
        // 创建新的context
        QueryContext context = new QueryContext(new QueryParam(param));
        contextMap.put(context.id, context);
        return context;
    }

    @NonNull
    private ResultVO getResultsWithTags(QueryContext context, int page) {
        ResultVO result = new ResultVO(context.id);
        // 预先添加额外的结果
        List<LogResult> exResults = context.getData(QUERY_PREFIX, C_KEY_EX_RESULT);
        Optional.ofNullable(exResults).ifPresent(result.logResults::addAll);

        int curPage = page, countLimit = context.queryParam.getCountLimit();
        while (true) {
            List<LogResult> temp = innerGetResultsWithTags(context, curPage);
            // 如果已无更多，则不再继续
            if (temp.isEmpty()) {
                break;
            }
            curPage++;
            int gap = countLimit - result.logResults.size();
            if (temp.size() >= gap) {
                // 只添加缺口数量的结果
                result.logResults.addAll(temp.subList(0, gap));
                // 剩余的结果保存到context
                context.putData(QUERY_PREFIX, C_KEY_REST_RESULT, temp.subList(gap, temp.size()));
                break;
            }
            result.logResults.addAll(temp);
        }
        // 记录下标
        context.putData(QUERY_PREFIX, C_KEY_TAG_END_PAGE, curPage);
        return result;
    }

    private List<LogResult> innerGetResultsWithTags(QueryContext context, int page) {
        // 使用标签过滤
        List<String> uidList = tagInfoService.getMatchedUidList(context, page);
        if (uidList.isEmpty()) {
            return Collections.emptyList();
        }
        // 回表，查找记录
        List<LogResult> result = new LinkedList<>();
        for (String uid : uidList) {
            List<LogLineInfo> logLineList = logLineInfoRepository.findByUidOrderByTime(uid);
            result.add(toResultVO(uid, logLineList));
        }
        // 使用过滤器过滤
        for (LogResult vo : result) {
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
    private ResultVO getResultsWithoutTags(QueryContext context, int page) {
        // todo 待完成
        return new ResultVO(context.id);
    }

    private LogResult toResultVO(String uid, List<LogLineInfo> list) {
        LogResult vo = new LogResult();
        vo.uid = uid;
        vo.sections = new LinkedList<>();
        LogSection section = new LogSection();
        section.logs = new LinkedList<>();
        vo.sections.add(section);
        for (LogLineInfo info : list) {
            section.logs.add(info.lineFromByte + "~" + info.toByte);
        }
        return vo;
    }

}
