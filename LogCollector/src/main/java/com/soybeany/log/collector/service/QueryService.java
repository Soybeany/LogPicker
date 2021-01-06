package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.QueryParam;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.LogLineInfoRepository;
import com.soybeany.log.collector.service.filter.LogFilter;
import com.soybeany.log.collector.service.limiter.LogLimiter;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogResult;
import com.soybeany.log.core.model.LogSection;
import com.soybeany.log.core.model.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

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
class QueryServiceImpl implements QueryService {

    private static final String QUERY_PREFIX = "query";

    private static final String P_KEY_CONTEXT_ID = "contextId"; // 关联context的id，string

    private static final String C_KEY_START_PAGE = "startPage"; // 开始分页下标(包含)，int
    private static final String C_KEY_END_PAGE = "endPage"; // 结束分页下标(不包含)，int
    private static final String C_KEY_EX_RESULT = "exResult"; // 额外的结果，即上一次查询超出数目限制的结果，List<ResultVO>
    private static final String C_KEY_REST_RESULT = "restResult"; // 剩余的结果，即本次查询剩余的结果，List<ResultVO>

    private static final String T_KEY_NEED_PAGING = "needPaging"; // 需要使用分页，String

    @Autowired
    private TagInfoService tagInfoService;
    @Autowired
    private LogResultService logResultService;
    @Autowired
    private LogLineInfoRepository logLineInfoRepository;
    @Autowired
    private List<LogFilter> logFilters;
    @Autowired
    private List<LogLimiter> logLimiters;
    @Autowired
    private List<QueryContext.IListener> queryContextListeners;
    @Autowired
    private List<QueryContext.NextContextHandler> nextContextHandlers;

    private final Map<String, QueryContext> contextMap = new ConcurrentHashMap<>();

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
            // 获取查询结果
            context.result = getResult(context);
            // 按需分页
            checkPageable(context);
            // 补充链接
            context.result.lastContextId = context.lastId;
            context.result.nextContextId = context.nextId;
            return context.result;
        } finally {
            context.clearTempData();
            // 释放锁
            context.lock.unlock();
        }
    }

    // ********************内部方法********************

    private int getPage(QueryContext context) {
        Integer page = context.getData(QUERY_PREFIX, C_KEY_START_PAGE);
        if (null == page) {
            context.putData(QUERY_PREFIX, C_KEY_START_PAGE, page = 0);
        }
        return page;
    }

    private QueryContext getQueryContext(Map<String, String> param) {
        String contextId = QueryParam.getParam(QUERY_PREFIX, P_KEY_CONTEXT_ID, param);
        // 若指定了contextId，则尝试复用context
        QueryContext context;
        if (null != contextId) {
            if (!contextMap.containsKey(contextId)) {
                throw new LogException("找不到指定contextId对应的context");
            }
            context = contextMap.get(contextId);
        }
        // 创建新的context
        else {
            context = new QueryContext(new QueryParam(param));
            contextMap.put(context.id, context);
        }
        // 回调监听器
        for (QueryContext.IListener listener : queryContextListeners) {
            listener.onInitTempData(context);
        }
        return context;
    }

    @NonNull
    private ResultVO getResult(QueryContext context) {
        ResultVO result = new ResultVO(context.id);
        // 预先添加额外的结果
        boolean needSearch = addExResults(context, result);
        int page = getPage(context);
        if (needSearch) {
            LogResultProvider provider = tagInfoService.hasParams(context)
                    ? this::getLogResultsWithTags
                    : this::getLogResultsWithoutTags;
            page = searchResults(context, result, page, provider);
        }
        // 记录下标
        context.putData(QUERY_PREFIX, C_KEY_END_PAGE, page);
        return result;
    }

    @NonNull
    private List<LogResult> getLogResultsWithTags(QueryContext context, int page) {
        // 使用标签过滤
        List<String> uidList = tagInfoService.getMatchedUidList(context, page, context.queryParam.getCountLimit());
        if (uidList.isEmpty()) {
            return Collections.emptyList();
        }
        // 回表，查找记录
        List<LogResult> result = new LinkedList<>();
        for (String uid : uidList) {
            List<LogLineInfo> logLineList = logLineInfoRepository.findByUidOrderByTime(uid);
            result.addAll(logResultService.toResults(context, logLineList));
        }
        return result;
    }

    @NonNull
    private List<LogResult> getLogResultsWithoutTags(QueryContext context, int page) {
        QueryParam queryParam = context.queryParam;
        Pageable pageable = PageRequest.of(page, context.queryParam.getCountLimit());
        List<LogLineInfo> list = logLineInfoRepository.findByTimeBetweenOrderByTime(queryParam.getFromTime(), queryParam.getToTime(), pageable);
        return logResultService.toResults(context, list);
    }

    @NonNull
    private int searchResults(QueryContext context, ResultVO result, int page, LogResultProvider provider) {
        while (true) {
            List<LogResult> temp = provider.getLogResults(context, page);
            // 如果已无更多，则不再继续
            if (temp.isEmpty()) {
                break;
            }
            // 使用过滤器过滤记录
            filterLogResults(context, temp);
            page++;
            // 日志条目限制
            for (int i = 0; i < temp.size(); i++) {
                if (shouldLimit(context, temp, i)) {
                    return page;
                }
                result.logResults.add(temp.get(i));
            }
        }
        return page;
    }

    /**
     * @return 是否需要搜索更多结果
     */
    private boolean addExResults(QueryContext context, ResultVO result) {
        List<LogResult> exResults = context.getData(QUERY_PREFIX, C_KEY_EX_RESULT);
        if (null == exResults) {
            return true;
        }
        for (int i = 0; i < exResults.size(); i++) {
            if (shouldLimit(context, exResults, i)) {
                return false;
            }
            result.logResults.add(exResults.get(i));
        }
        return true;
    }

    private boolean shouldLimit(QueryContext context, List<LogResult> list, int index) {
        LogResult logResult = list.get(index);
        for (LogLimiter limiter : logLimiters) {
            if (!limiter.shouldAddResult(context, logResult)) {
                context.putTempData(QUERY_PREFIX, T_KEY_NEED_PAGING, "已达" + limiter.getDesc());
                // 剩余的结果保存到context
                context.putData(QUERY_PREFIX, C_KEY_REST_RESULT, list.subList(index, list.size()));
                return true;
            }
        }
        return false;
    }

    private void filterLogResults(QueryContext context, List<LogResult> result) {
        Iterator<LogResult> resultIterator = result.iterator();
        while (resultIterator.hasNext()) {
            LogResult vo = resultIterator.next();
            Iterator<LogSection> sectionIterator = vo.sections.iterator();
            // 使用过滤器对结果进行过滤
            while (sectionIterator.hasNext()) {
                LogSection section = sectionIterator.next();
                for (LogFilter filter : logFilters) {
                    if (filter.shouldFilter(context, section)) {
                        sectionIterator.remove();
                    }
                }
            }
            // 若分区无内容，则移除此条结果
            if (vo.sections.isEmpty()) {
                resultIterator.remove();
            }
        }
    }

    private void checkPageable(QueryContext context) {
        String pagingReason = context.getTempData(QUERY_PREFIX, T_KEY_NEED_PAGING);
        if (null == pagingReason) {
            return;
        }
        context.result.endReason = pagingReason;
        // 设置nextContext
        QueryContext nextContext = new QueryContext(context.queryParam);
        contextMap.put(nextContext.id, nextContext);
        nextContextHandlers.forEach(handler -> handler.onHandleNextContext(context, nextContext));
        context.nextId = nextContext.id;
        nextContext.lastId = context.id;
    }

    // ********************内部类********************

    @Component
    static class NextContextHandler implements QueryContext.NextContextHandler {
        @Override
        public void onHandleNextContext(QueryContext old, QueryContext next) {
            // 标签分页
            Integer endPage = old.getData(QUERY_PREFIX, C_KEY_END_PAGE);
            next.putData(QUERY_PREFIX, C_KEY_START_PAGE, endPage);
            // 查询结果
            List<ResultVO> restResult = old.getData(QUERY_PREFIX, C_KEY_REST_RESULT);
            next.putData(QUERY_PREFIX, C_KEY_EX_RESULT, restResult);
        }
    }

    private interface LogResultProvider {
        List<LogResult> getLogResults(QueryContext context, int page);
    }

}
