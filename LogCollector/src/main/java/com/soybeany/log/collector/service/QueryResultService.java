package com.soybeany.log.collector.service;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.model.LogLine;
import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.RawLogResult;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.service.converter.LogLineConvertService;
import com.soybeany.log.collector.service.converter.LogResultConvertService;
import com.soybeany.log.collector.service.converter.RawLogResultConvertService;
import com.soybeany.log.collector.service.filter.LogFilter;
import com.soybeany.log.collector.service.limiter.LogLimiter;
import com.soybeany.log.core.model.LogResult;
import com.soybeany.log.core.model.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public interface QueryResultService {

    @NonNull
    ResultVO getResult(QueryContext context);

}

@Service
class QueryResultServiceImpl implements QueryResultService {

    private static final String PREFIX = "queryResult";

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
    private LogLineConvertService logLineConvertService;
    @Autowired
    private RawLogResultConvertService rawLogResultConvertService;
    @Autowired
    private LogResultConvertService logResultConvertService;
    @Autowired
    private List<QueryContext.NextContextHandler> nextContextHandlers;
    @Autowired
    private List<LogFilter> logFilters;
    @Autowired
    private List<LogLimiter> logLimiters;

    @Override
    public ResultVO getResult(QueryContext context) {
        List<RawLogResult> formalList = new LinkedList<>();
        // 预先添加额外的结果
        boolean needSearch = addExResults(context, formalList);
        int page = getPage(context);
        if (needSearch) {
            page = searchResults(context, formalList, page);
        }
        // 记录下标
        context.putData(PREFIX, C_KEY_END_PAGE, page);
        // 转换为结果
        return toResultVO(context, formalList);
    }

    // ********************内部方法********************

    @PostConstruct
    private void onInit() {
        Collections.sort(nextContextHandlers);
    }

    private int getPage(QueryContext context) {
        Integer page = context.getData(PREFIX, C_KEY_START_PAGE);
        if (null == page) {
            context.putData(PREFIX, C_KEY_START_PAGE, page = 0);
        }
        return page;
    }

    @NonNull
    private int searchResults(QueryContext context, List<RawLogResult> formalList, int page) {
        int pageSize = Math.min(appConfig.maxPageSize, context.queryParam.getCountLimit());
        while (true) {
            List<LogLineInfo> infoList = logSelectorService.select(context, page, pageSize);
            // 如果已无更多，则不再继续
            if (null == infoList || infoList.isEmpty()) {
                break;
            }
            // 对象转换
            List<LogLine> logLines = logLineConvertService.convert(context, infoList);
            List<RawLogResult> tempResults = rawLogResultConvertService.convert(context, logLines);
            // 使用过滤器过滤记录
            filterLogResults(context, tempResults);
            page++;
            // 结果限制
            boolean isLimited = addResultsAndReturnIsLimited(context, tempResults, formalList);
            if (isLimited) {
                return page;
            }
        }
        return page;
    }

    /**
     * @return 是否需要搜索更多结果
     */
    private boolean addExResults(QueryContext context, List<RawLogResult> formalList) {
        List<RawLogResult> exResults = context.getData(PREFIX, C_KEY_EX_RESULT);
        if (null == exResults) {
            return true;
        }
        return !addResultsAndReturnIsLimited(context, exResults, formalList);
    }

    /**
     * @return 是否对结果进行了限制
     */
    private boolean addResultsAndReturnIsLimited(QueryContext context, List<RawLogResult> tempList, List<RawLogResult> formalList) {
        for (int i = 0; i < tempList.size(); i++) {
            RawLogResult logResult = tempList.get(i);
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

    private void filterLogResults(QueryContext context, List<RawLogResult> results) {
        Iterator<RawLogResult> iterator = results.iterator();
        while (iterator.hasNext()) {
            RawLogResult result = iterator.next();
            for (LogFilter filter : logFilters) {
                if (filter.shouldFilter(context, result)) {
                    iterator.remove();
                }
            }
        }
    }

    private void checkPageable(QueryContext context) {
        String pagingReason = context.getTempData(PREFIX, T_KEY_NEED_PAGING);
        if (null == pagingReason) {
            return;
        }
        context.result.endReason = pagingReason;
        // 设置nextContext
        QueryContext nextContext = queryContextService.createNewNextContextOf(context);
        nextContextHandlers.forEach(handler -> handler.onHandleNextContext(context, nextContext));
        context.nextId = nextContext.id;
        nextContext.lastId = context.id;
    }

    private ResultVO toResultVO(QueryContext context, List<RawLogResult> formalList) {
        // 按需分页
        checkPageable(context);
        // 创建Result
        List<LogResult> results = logResultConvertService.convert(context, formalList);
        return new ResultVO(context.lastId, context.id, context.nextId, results);
    }

    // ********************内部类********************

    @Component
    static class NextContextHandler implements QueryContext.NextContextHandler {
        @Override
        public void onHandleNextContext(QueryContext old, QueryContext next) {
            // 标签分页
            Integer endPage = old.getData(PREFIX, C_KEY_END_PAGE);
            next.putData(PREFIX, C_KEY_START_PAGE, endPage);
            // 查询结果
            List<RawLogResult> restResult = old.getData(PREFIX, C_KEY_REST_RESULT);
            next.putData(PREFIX, C_KEY_EX_RESULT, restResult);
        }
    }

}
