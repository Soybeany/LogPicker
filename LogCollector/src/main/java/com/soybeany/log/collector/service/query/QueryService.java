package com.soybeany.log.collector.service.query;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.BytesRangeService;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.common.model.loader.ILogLineLoader;
import com.soybeany.log.collector.service.common.model.loader.RangesLogLineLoader;
import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.collector.service.query.data.QueryParam;
import com.soybeany.log.collector.service.query.exporter.LogExporter;
import com.soybeany.log.collector.service.query.filter.LogFilterFactory;
import com.soybeany.log.collector.service.query.model.ILogFilter;
import com.soybeany.log.collector.service.query.model.LogPackLoader;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
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

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private QueryContextService queryContextService;
    @Autowired
    private BytesRangeService bytesRangeService;
    @Autowired
    private List<LogFilterFactory> logFilterFactories;
    @Autowired
    private LogExporter logExporter;
    @Autowired
    private LogIndexes.Updater indexesUpdater;

    @Override
    public String simpleQuery(Map<String, String> param) {
        QueryContext context = getContext(param);
        try {
            // 获取锁
            context.lock.lock();
            // 如果context中已包含结果，则直接返回
            if (null != context.result) {
                return context.result;
            }
            return context.result = query(context);
        } catch (Exception e) {
            throw new LogException(e);
        } finally {
            context.clearTempData();
            // 释放锁
            context.lock.unlock();
        }
    }

    // ********************内部方法********************

    @NonNull
    private QueryContext getContext(Map<String, String> param) {
        QueryContext context = queryContextService.loadContextFromParam(param);
        // 若已有context，则直接使用
        if (null != context) {
            return context;
        }
        // 若还没有，则创建新context
        QueryParam queryParam = new QueryParam(appConfig, param);
        context = new QueryContext(queryParam);
        queryContextService.registerContext(context);
        for (LogFilterFactory factory : logFilterFactories) {
            ILogFilter filter = factory.getNewLogFilterIfInNeed(context);
            if (null != filter) {
                context.filters.add(filter);
            }
        }
        for (File logFile : queryParam.getLogFiles()) {
            // 更新索引
            LogIndexes indexes = indexesUpdater.updateAndGet(logFile);
            // 保存待查询的范围
            context.pathMap.put(logFile.getAbsolutePath(), getRanges(context, indexes));
        }
        return context;
    }

    private List<FileRange> getRanges(QueryContext context, LogIndexes indexes) {
        List<List<FileRange>> rangesList = new LinkedList<>();
        // 添加默认的全文查找
        rangesList.add(Collections.singletonList(new FileRange(0, indexes.logFile.length())));
        // 添加各过滤器指定的范围
        for (ILogFilter filter : context.filters) {
            rangesList.add(filter.getFilteredRanges(indexes));
        }
        // 得到交集
        return bytesRangeService.intersect(rangesList);
    }

    private String query(QueryContext context) throws IOException {
        List<LogPack> results = new LinkedList<>();
        // 如果还有查询范围，则继续查询
        boolean needMoreResults = queryByScan(context, results);
        // 如果记录数不够，则继续遍历临时列表并弹出记录
        if (needMoreResults) {
            queryByPopUidMap(context, results);
        }
        // 返回结果
        return exportLogs(context, results);
    }

    /**
     * 是否需要继续添加记录
     */
    private boolean queryByScan(QueryContext context, List<LogPack> results) throws IOException {
        Iterator<String> iterator = context.pathMap.keySet().iterator();
        while (iterator.hasNext()) {
            String path = iterator.next();
            List<FileRange> ranges = context.pathMap.get(path);
            boolean canStop = false;
            long readPointer;
            // 在范围中查找
            try (ILogLineLoader lineLoader = new RangesLogLineLoader(new File(path), appConfig.logCharset,
                    appConfig.lineParsePattern, appConfig.tagParsePattern, ranges);
                 LogPackLoader packLoader = new LogPackLoader(lineLoader, appConfig.maxLinesPerResultWithNullUid, context.uidMap)) {
                LogPack logPack;
                while (!canStop && null != (logPack = packLoader.loadNextCompleteLogPack())) {
                    canStop = tryAddToList(context, results, logPack);
                }
                readPointer = lineLoader.getReadPointer();
            }
            // 使用已查询到的字节，再次进行范围合并，若合并后范围为空，则移除此范围
            List<FileRange> nextRange = Collections.singletonList(new FileRange(readPointer, Long.MAX_VALUE));
            List<FileRange> newRanges = bytesRangeService.intersect(Arrays.asList(ranges, nextRange));
            if (newRanges.isEmpty()) {
                iterator.remove();
            } else {
                context.pathMap.put(path, newRanges);
            }
            // 如果状态不为继续，则中断
            if (canStop) {
                return false;
            }
        }
        return true;
    }

    private void queryByPopUidMap(QueryContext context, List<LogPack> results) {
        Iterator<LogPack> iterator = context.uidMap.values().iterator();
        while (iterator.hasNext()) {
            LogPack logPack = iterator.next();
            iterator.remove();
            boolean canStop = tryAddToList(context, results, logPack);
            if (canStop) {
                return;
            }
        }
    }

    /**
     * @return 是否可以返回结果
     */
    private boolean tryAddToList(QueryContext context, List<LogPack> results, LogPack candidate) {
        // 筛选
        if (shouldFilter(context, candidate)) {
            return false;
        }
        // 添加到结果列表
        results.add(candidate);
        // 是否已达要求的结果数
        boolean canStop = context.queryParam.getCountLimit() == results.size();
        if (canStop) {
            context.endReason = "已找到指定数目的结果";
        }
        return canStop;
    }

    private boolean shouldFilter(QueryContext context, LogPack logPack) {
        for (ILogFilter filter : context.filters) {
            if (filter.filterLogPack(logPack)) {
                return true;
            }
        }
        return false;
    }

    private String exportLogs(QueryContext context, List<LogPack> formalList) {
        // 按需分页
        setPageable(context);
        // 日志排序
        for (LogPack logPack : formalList) {
            Collections.sort(logPack.logLines);
        }
        // 导出日志
        return logExporter.export(context, formalList);
    }

    private void setPageable(QueryContext context) {
        // 若查询范围、临时列表均为空，则不需要分页
        if (context.pathMap.isEmpty() && context.uidMap.isEmpty()) {
            return;
        }
        // 创建并绑定下一分页
        QueryContext nextContext = new QueryContext(context);
        context.nextId = nextContext.id;
        nextContext.lastId = context.id;
        queryContextService.registerContext(nextContext);
    }

}
