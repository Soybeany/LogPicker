package com.soybeany.log.collector.service.query;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.BytesRangeService;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.common.model.LogFilter;
import com.soybeany.log.collector.service.common.model.loader.LogPackLoader;
import com.soybeany.log.collector.service.common.model.loader.RangesLogLineLoader;
import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.collector.service.query.data.QueryParam;
import com.soybeany.log.collector.service.query.exporter.LogExporter;
import com.soybeany.log.collector.service.query.filter.LogFilterFactory;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.util.file.BdFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.Closeable;
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
            LogFilter filter = factory.getNewLogFilterIfInNeed(context);
            if (null != filter) {
                context.filters.add(filter);
            }
        }
        for (File logFile : queryParam.getLogFiles()) {
            // 更新索引
            LogIndexes indexes = indexesUpdater.updateAndGet(logFile);
            // 设置待查询的范围
            FileRange timeRange = getTimeRange(indexes, queryParam.getFromTime(), queryParam.getToTime());
            for (LogFilter filter : context.filters) {
                filter.onSetupRanges(context.uidMap, timeRange, indexes);
            }
        }
        return context;
    }

    private String query(QueryContext context) throws IOException {
        List<LogPack> results = new LinkedList<>();
        // 如果还有查询范围，则继续查询
        boolean needMoreResults;
        // 创建可复用的加载器持有器，遇到相同file时不需重新创建
        Map<File, LoaderHolder> holderMap = new HashMap<>();
        try {
            needMoreResults = queryByScan(context, results, holderMap);
        } finally {
            // 释放加载器持有器
            for (LoaderHolder holder : holderMap.values()) {
                BdFileUtils.closeStream(holder);
            }
        }
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
    private boolean queryByScan(QueryContext context, List<LogPack> results, Map<File, LoaderHolder> holderMap) throws IOException {
        Iterator<String> uidIterator = context.uidMap.keySet().iterator();
        // uid的遍历
        while (uidIterator.hasNext()) {
            String uid = uidIterator.next();
            Map<File, List<FileRange>> fileRangeMap = context.uidMap.get(uid);
            Iterator<File> fileIterator = fileRangeMap.keySet().iterator();
            // 不同文件的遍历
            while (fileIterator.hasNext()) {
                boolean canStop = executeScanQuery(context, results, holderMap,
                        uidIterator, fileIterator, uid, fileRangeMap);
                // 如果可以停止，则中断
                if (canStop) {
                    return false;
                }
            }
        }
        return true;
    }

    private void queryByPopUidMap(QueryContext context, List<LogPack> results) {
        Iterator<LogPack> iterator = context.uidTempMap.values().iterator();
        while (iterator.hasNext()) {
            LogPack logPack = iterator.next();
            iterator.remove();
            boolean canStop = tryAddToList(context, results, logPack);
            if (canStop) {
                return;
            }
        }
    }

    private boolean executeScanQuery(QueryContext context, List<LogPack> results,
                                     Map<File, LoaderHolder> holderMap, Iterator<String> uidIterator, Iterator<File> fileIterator,
                                     String uid, Map<File, List<FileRange>> fileRangeMap) throws IOException {
        boolean canStop = false;
        File file = fileIterator.next();
        List<FileRange> ranges = fileRangeMap.get(file);
        LoaderHolder holder = holderMap.get(file);
        // 按需创建持有器
        if (null == holder) {
            RangesLogLineLoader lineLoader = new RangesLogLineLoader(file, appConfig.logCharset,
                    appConfig.lineParsePattern, appConfig.tagParsePattern);
            LogPackLoader packLoader = new LogPackLoader(lineLoader, appConfig.maxLinesPerResultWithNullUid, context.uidTempMap);
            holderMap.put(file, holder = new LoaderHolder(lineLoader, packLoader));
        }
        // 切换范围后查找
        holder.logLineLoader.switchRanges(ranges);
        LogPack logPack;
        while (!canStop && null != (logPack = holder.logPackLoader.loadNextCompleteLogPack())) {
            // 若不是待查找的uid记录，则不作处理
            if (!uid.equals(logPack.uid)) {
                continue;
            }
            canStop = tryAddToList(context, results, logPack);
        }
        long readPointer = holder.logLineLoader.getReadPointer();
        // 使用已查询到的字节，再次进行范围合并
        List<FileRange> nextRange = Collections.singletonList(new FileRange(readPointer, Long.MAX_VALUE));
        List<FileRange> newRanges = bytesRangeService.intersect(Arrays.asList(ranges, nextRange));
        // 合并后范围为空，则移除该范围
        if (newRanges.isEmpty()) {
            fileIterator.remove();
        } else {
            fileRangeMap.put(file, newRanges);
        }
        // 若全部范围为空，则移除该uid
        if (fileRangeMap.isEmpty()) {
            uidIterator.remove();
        }
        return canStop;
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
        for (LogFilter filter : context.filters) {
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

    private FileRange getTimeRange(LogIndexes indexes, String fromTime, String toTime) {
        TreeMap<String, Long> timeIndexMap = indexes.timeIndexMap;
        long startByte = Optional.ofNullable(timeIndexMap.floorEntry(fromTime))
                .map(Map.Entry::getValue).orElse(0L);
        long endByte = Optional.ofNullable(timeIndexMap.ceilingEntry(toTime))
                .map(Map.Entry::getValue).orElse(indexes.scannedBytes);
        return new FileRange(startByte, endByte);
    }

    private void setPageable(QueryContext context) {
        // 若查询范围、临时列表均为空，则不需要分页
        if (context.uidMap.isEmpty() && context.uidTempMap.isEmpty()) {
            return;
        }
        // 创建并绑定下一分页
        QueryContext nextContext = new QueryContext(context);
        context.nextId = nextContext.id;
        nextContext.lastId = context.id;
        queryContextService.registerContext(nextContext);
    }

    // ********************内部类********************

    private static class LoaderHolder implements Closeable {
        RangesLogLineLoader logLineLoader;
        LogPackLoader logPackLoader;

        public LoaderHolder(RangesLogLineLoader logLineLoader, LogPackLoader logPackLoader) {
            this.logLineLoader = logLineLoader;
            this.logPackLoader = logPackLoader;
        }

        @Override
        public void close() {
            BdFileUtils.closeStream(logLineLoader);
            BdFileUtils.closeStream(logPackLoader);
        }
    }

}
