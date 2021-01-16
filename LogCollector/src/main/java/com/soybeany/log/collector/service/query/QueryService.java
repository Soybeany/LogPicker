package com.soybeany.log.collector.service.query;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.query.exporter.LogExporter;
import com.soybeany.log.collector.service.query.filter.LogFilter;
import com.soybeany.log.collector.service.query.limiter.LogLimiter;
import com.soybeany.log.collector.service.query.model.IQueryListener;
import com.soybeany.log.collector.service.query.model.QueryContext;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogPack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface QueryService {

    @NonNull
    String simpleQuery(Map<String, String> param);

}

@Service
class QueryServiceImpl implements QueryService {

    private static final String PREFIX = "query";

    private static final String C_KEY_START_PAGE = "startPage"; // 开始分页下标(包含)，int
    private static final String C_KEY_END_PAGE = "endPage"; // 结束分页下标(不包含)，int
    private static final String C_KEY_EX_RESULT = "exResult"; // 额外的结果，即上一次查询超出数目限制的结果，List<RawLogResult>
    private static final String C_KEY_REST_RESULT = "restResult"; // 剩余的结果，即本次查询剩余的结果，List<RawLogResult>

    private static final String T_KEY_NEED_PAGING = "needPaging"; // 需要使用分页，String

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private QueryContextService queryContextService;
    @Autowired
    private LogSelectorService logSelectorService;
    @Autowired
    private LogCutterService logCutterService;
    @Autowired
    private List<LogFilter> logFilters;
    @Autowired
    private List<LogLimiter> logLimiters;
    @Autowired
    private List<IQueryListener> queryListeners;
    @Autowired
    private LogExporter logExporter;

    @Override
    public String simpleQuery(Map<String, String> param) {
        QueryContext context = queryContextService.initFromParam(param);
        try {
            // 获取锁
            context.lock.lock();
            // 如果context中已包含结果，则直接返回
            if (null != context.result) {
                return context.result;
            }
            return context.result = query(context);
        } finally {
            context.clearTempData();
            // 释放锁
            context.lock.unlock();
        }
    }

    // ********************内部方法********************

    @PostConstruct
    private void onInit() {
        Collections.sort(queryListeners);
    }

    private String query(QueryContext context) {
        // 回调监听器
        for (IQueryListener listener : queryListeners) {
            listener.onQuery(context);
        }
        List<LogPack> formalPacks = new LinkedList<>();
        // 预先添加额外的结果
        boolean needSearch = addExResults(context, formalPacks);
        int page = getPage(context);
        if (needSearch) {
            page = searchResults(context, formalPacks, page);
        }
        // 记录下标
        context.putData(PREFIX, C_KEY_END_PAGE, page);
        // 导出日志
        return exportLogs(context, formalPacks);
    }

    private int getPage(QueryContext context) {
        Integer page = context.getData(PREFIX, C_KEY_START_PAGE);
        if (null == page) {
            context.putData(PREFIX, C_KEY_START_PAGE, page = 0);
        }
        return page;
    }

    @NonNull
    private int searchResults(QueryContext context, List<LogPack> formalPacks, int page) {
        int pageSize = Math.min(appConfig.maxPageSize, context.queryParam.getCountLimit());
        while (true) {
            List<LogLine> logLines = logSelectorService.select(context, page, pageSize);
            // 如果已无更多，则不再继续
            if (logLines.isEmpty()) {
                break;
            }
            // 分割日志
            List<LogPack> tempPacks = logCutterService.cut(context, logLines);
            // 使用过滤器过滤记录
            filterLogPacks(context, tempPacks);
            page++;
            // 结果限制
            boolean isLimited = addResultsAndReturnIsLimited(context, tempPacks, formalPacks);
            if (isLimited) {
                return page;
            }
        }
        return page;
    }

    /**
     * @return 是否需要搜索更多结果
     */
    private boolean addExResults(QueryContext context, List<LogPack> formalList) {
        List<LogPack> exResults = context.getData(PREFIX, C_KEY_EX_RESULT);
        if (null == exResults) {
            return true;
        }
        return !addResultsAndReturnIsLimited(context, exResults, formalList);
    }

    /**
     * @return 是否对结果进行了限制
     */
    private boolean addResultsAndReturnIsLimited(QueryContext context, List<LogPack> tempList, List<LogPack> formalList) {
        for (int i = 0; i < tempList.size(); i++) {
            LogPack logResult = tempList.get(i);
            for (LogLimiter limiter : logLimiters) {
                if (limiter.canAddResult(context, logResult)) {
                    continue;
                }
                context.putTempData(PREFIX, T_KEY_NEED_PAGING, "已达" + limiter.getDesc());
                // 剩余的结果保存到context
                context.putData(PREFIX, C_KEY_REST_RESULT, tempList.subList(i, tempList.size()));
                return true;
            }
            formalList.add(logResult);
        }
        return false;
    }

    private void filterLogPacks(QueryContext context, List<LogPack> packs) {
        Iterator<LogPack> iterator = packs.iterator();
        while (iterator.hasNext()) {
            LogPack result = iterator.next();
            for (LogFilter filter : logFilters) {
                if (filter.shouldFilter(context, result)) {
                    iterator.remove();
                }
            }
        }
    }

    private String exportLogs(QueryContext context, List<LogPack> formalList) {
        // 按需分页
        checkPageable(context);
        // 导出日志
        return logExporter.export(context, formalList);
    }

    private void checkPageable(QueryContext context) {
        String pagingReason = context.getTempData(PREFIX, T_KEY_NEED_PAGING);
        if (null == pagingReason) {
            return;
        }
        context.endReason = pagingReason;
        // 设置nextContext
        QueryContext nextContext = queryContextService.createNewNextContextOf(context);
        queryListeners.forEach(handler -> handler.onHandleNextContext(context, nextContext));
        context.nextId = nextContext.id;
        nextContext.lastId = context.id;
    }

    // ********************内部类********************

    @Component
    static class NextContextHandler implements IQueryListener {
        @Override
        public void onHandleNextContext(QueryContext old, QueryContext next) {
            // 标签分页
            Integer endPage = old.getData(PREFIX, C_KEY_END_PAGE);
            next.putData(PREFIX, C_KEY_START_PAGE, endPage);
            // 查询结果
            List<LogPack> restResult = old.getData(PREFIX, C_KEY_REST_RESULT);
            next.putData(PREFIX, C_KEY_EX_RESULT, restResult);
        }
    }

}
