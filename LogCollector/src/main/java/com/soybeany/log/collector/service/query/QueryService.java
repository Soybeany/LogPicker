package com.soybeany.log.collector.service.query;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.BytesRangeService;
import com.soybeany.log.collector.service.common.LogLoaderService;
import com.soybeany.log.collector.service.common.model.FileRange;
import com.soybeany.log.collector.service.common.model.LogIndexes;
import com.soybeany.log.collector.service.query.exporter.LogExporter;
import com.soybeany.log.collector.service.query.filter.LogFilter;
import com.soybeany.log.collector.service.query.model.ILogPackReceiver;
import com.soybeany.log.collector.service.query.model.LogReceiverAdapter;
import com.soybeany.log.collector.service.query.model.QueryContext;
import com.soybeany.log.collector.service.query.model.QueryParam;
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

    private static final String PREFIX = "query";
    private static final String P_KEY_FILES = "files"; // 需要查询文件的路径，使用“;”分隔，String

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private QueryContextService queryContextService;
    @Autowired
    private LogLoaderService logLoaderService;
    @Autowired
    private BytesRangeService bytesRangeService;
    @Autowired
    private TagInfoService tagInfoService;
    @Autowired
    private List<LogFilter> logFilters;
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
            throw new LogException(e.getMessage());
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
        for (String filePath : context.getParam(PREFIX, P_KEY_FILES).split(";")) {
            // 更新索引
            LogIndexes indexes = indexesUpdater.updateAndGet(new File(filePath));
            // 保存待查询的范围
            context.pathMap.put(filePath, getRanges(indexes, queryParam));
        }
        return context;
    }

    private List<FileRange> getRanges(LogIndexes indexes, QueryParam param) {
        // 如果没有指定标签，则进行全文查询
        if (!tagInfoService.hasTags(param)) {
            return Collections.singletonList(new FileRange(0, indexes.logFile.length()));
        }
        // 否则返回使用tag筛选后的范围列表
        return tagInfoService.getIntersectedRanges(indexes, param);
    }

    private String query(QueryContext context) throws IOException {
        List<LogPack> results = new LinkedList<>();
        // 如果还有查询范围，则继续查询
        if (!context.pathMap.isEmpty()) {
            queryByScan(context, results);
        }
        // 否则遍历临时列表弹出记录
        else {
            queryByPopUidMap(context, results);
        }
        // 返回结果
        return exportLogs(context, results);
    }

    private void queryByScan(QueryContext context, List<LogPack> results) throws IOException {
        for (String path : context.pathMap.keySet()) {
            List<FileRange> ranges = context.pathMap.get(path);
            // 在范围中查找
            LogPackReceiver receiver = new LogPackReceiver(context, results);
            LogReceiverAdapter adapter = new LogReceiverAdapter(appConfig.maxLinesPerResultWithNullUid, context.uidMap, receiver);
            boolean canStop = logLoaderService.load(new File(path), ranges, adapter);
            // 使用已查询到的字节，再次进行范围合并，若合并后范围为空，则移除此范围
            List<FileRange> nextRange = Collections.singletonList(new FileRange(receiver.actualEndPointer, Long.MAX_VALUE));
            ranges = bytesRangeService.intersect(Arrays.asList(ranges, nextRange));
            if (ranges.isEmpty()) {
                context.pathMap.remove(path);
            } else {
                context.pathMap.put(path, ranges);
            }
            // 如果状态不为继续，则中断
            if (canStop) {
                return;
            }
        }
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
        boolean filtered = shouldFilter(context, candidate);
        if (filtered) {
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
        for (LogFilter filter : logFilters) {
            if (filter.shouldFilter(context, logPack)) {
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
        // 若查询范围为空，则不需要分页
        if (context.pathMap.isEmpty()) {
            return;
        }
        // 创建并绑定下一分页
        QueryContext nextContext = new QueryContext(context);
        context.nextId = nextContext.id;
        nextContext.lastId = context.id;
        queryContextService.registerContext(nextContext);
    }

    // ********************内部类********************

    private class LogPackReceiver implements ILogPackReceiver {
        private final QueryContext context;
        private final List<LogPack> logPacks;
        public long actualEndPointer = 0;

        public LogPackReceiver(QueryContext context, List<LogPack> logPacks) {
            this.context = context;
            this.logPacks = logPacks;
        }

        @Override
        public void onFinish(long bytesRead, long actualEndPointer) {
            this.actualEndPointer = actualEndPointer;
        }

        @Override
        public boolean onReceive(LogPack logPack) {
            return !tryAddToList(context, logPacks, logPack);
        }
    }
}
