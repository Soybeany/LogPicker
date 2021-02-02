package com.soybeany.log.collector.service.query;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.BytesRangeService;
import com.soybeany.log.collector.service.common.LogIndexService;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.common.model.loader.LogPackLoader;
import com.soybeany.log.collector.service.common.model.loader.RangesLogLineLoader;
import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.collector.service.query.data.QueryParam;
import com.soybeany.log.collector.service.query.data.QueryResult;
import com.soybeany.log.collector.service.query.exporter.LogExporter;
import com.soybeany.log.collector.service.query.factory.ModuleFactory;
import com.soybeany.log.collector.service.query.processor.LogFilter;
import com.soybeany.log.collector.service.query.processor.Preprocessor;
import com.soybeany.log.collector.service.query.processor.RangeLimiter;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.util.file.BdFileUtils;
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

    @NonNull
    String queryByUid(String uid);

}

@Service
class QueryServiceImpl implements QueryService {

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private QueryResultService queryResultService;
    @Autowired
    private BytesRangeService bytesRangeService;
    @Autowired
    private List<ModuleFactory> moduleFactories;
    @Autowired
    private LogExporter logExporter;
    @Autowired
    private LogIndexService logIndexService;
    @Autowired
    private LogIndexes.Updater indexesUpdater;

    @Override
    public String simpleQuery(Map<String, String> param) {
        QueryResult result = getResult(param);
        QueryContext context = result.context;
        try {
            // 获取锁
            result.lock();
            // 如果context中已包含结果，则直接返回
            if (null != result.text) {
                return result.text;
            }
            return result.text = query(result, context);
        } catch (Exception e) {
            throw new LogException(e);
        } finally {
            // 释放锁
            result.unlock();
        }
    }

    @Override
    public String queryByUid(String uid) {
        // todo 待实现
        return null;
    }

    // ********************内部方法********************

    @NonNull
    private QueryResult getResult(Map<String, String> param) {
        QueryResult result = queryResultService.loadResultFromParam(param);
        // 若已有context，则直接使用
        if (null != result) {
            return result;
        }
        // 若还没有，则创建新context
        return getNewResult(param);
    }

    private QueryResult getNewResult(Map<String, String> param) {
        QueryParam queryParam = new QueryParam(appConfig, param);
        QueryResult result = new QueryResult(queryParam);
        QueryContext context = result.context;
        queryResultService.registerResult(result);
        List<Preprocessor> preprocessors = new LinkedList<>();
        moduleFactories.forEach(factory -> factory.onSetupPreprocessors(context, preprocessors));
        List<RangeLimiter> limiters = new LinkedList<>();
        for (Preprocessor processor : preprocessors) {
            if (processor instanceof RangeLimiter) {
                limiters.add((RangeLimiter) processor);
            } else if (processor instanceof LogFilter) {
                context.filters.add((LogFilter) processor);
            } else {
                throw new LogException("使用了未知的Preprocessor");
            }
        }
        for (File logFile : queryParam.getLogFiles()) {
            // 更新并稳固索引
            LogIndexes indexes = indexesUpdater.updateAndGet(logFile);
            logIndexService.stabilize(indexes);
            context.indexesMap.put(logFile, indexes);
            // 设置待查询的范围
            FileRange timeRange = getTimeRange(indexes, queryParam.getFromTime(), queryParam.getToTime());
            List<List<FileRange>> rangeList = new LinkedList<>();
            rangeList.add(Collections.singletonList(timeRange));
            for (RangeLimiter limiter : limiters) {
                Optional.ofNullable(limiter.onSetupUnfilteredUidSet(timeRange, indexes)).ifPresent(context.unfilteredUidSet::addAll);
                Optional.ofNullable(limiter.onSetupQueryRanges(timeRange, indexes)).ifPresent(rangeList::add);
            }
            // 合并范围并保存到context
            List<FileRange> intersectedRanges = bytesRangeService.intersect(rangeList);
            if (!intersectedRanges.isEmpty()) {
                context.queryRanges.put(logFile, intersectedRanges);
            }
        }
        return result;
    }

    private String query(QueryResult result, QueryContext context) throws IOException {
        List<LogPack> formalLogPacks = new LinkedList<>();
        boolean needMore;
        // 如果有未使用的结果，则直接使用
        needMore = queryByUnusedResults(result, context, formalLogPacks);
        // 创建可复用的加载器持有器，遇到相同file时不需重新创建
        Map<File, LogPackLoader> loaderMap = new HashMap<>();
        try {
            if (needMore) {
                needMore = queryByUnfilteredUidSet(result, context, formalLogPacks, loaderMap);
            }
            if (needMore) {
                needMore = queryByRanges(result, context, formalLogPacks, loaderMap);
            }
        } finally {
            // 释放加载器持有器
            for (LogPackLoader loader : loaderMap.values()) {
                BdFileUtils.closeStream(loader);
            }
        }
        // 如果记录数不够，则继续遍历临时列表并弹出记录
        if (needMore) {
            queryByPopUidMap(result, context, formalLogPacks);
        }
        // 返回结果
        return exportLogs(result, context, formalLogPacks);
    }

    private void queryByPopUidMap(QueryResult result, QueryContext context, List<LogPack> formalLogPacks) {
        Iterator<LogPack> iterator = context.uidTempMap.values().iterator();
        while (iterator.hasNext()) {
            LogPack logPack = iterator.next();
            iterator.remove();
            if (context.usedUidSet.contains(logPack.uid)) {
                continue;
            }
            boolean needMore = filterAndAddToResults(result, context, formalLogPacks, logPack);
            if (!needMore) {
                return;
            }
        }
    }

    private boolean queryByRanges(QueryResult result, QueryContext context, List<LogPack> formalLogPacks, Map<File, LogPackLoader> loaderMap) throws IOException {
        Map<File, LogPackLoader> loaderMapForUid = new HashMap<>();
        boolean needMore = true;
        try {
            Iterator<Map.Entry<File, List<FileRange>>> iterator = context.queryRanges.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<File, List<FileRange>> entry = iterator.next();
                File file = entry.getKey();
                List<FileRange> ranges = entry.getValue();
                LogPackLoader loader = getLoader(loaderMap, file, context.uidTempMap);
                RangesLogLineLoader rangesLogLineLoader = (RangesLogLineLoader) loader.getLogLineLoader();
                rangesLogLineLoader.switchRanges(ranges);
                LogPack logPack;
                while (needMore && null != (logPack = loader.loadNextCompleteLogPack())) {
                    if (isFiltered(context, logPack)) {
                        continue;
                    }
                    if (null != logPack.uid) {
                        needMore = addResultsByUid(result, context, logPack.uid, formalLogPacks, loaderMapForUid);
                    } else {
                        needMore = filterAndAddToResults(result, context, formalLogPacks, logPack);
                    }
                }
                long readPointer = rangesLogLineLoader.getReadPointer();
                // 使用已查询到的字节，再次进行范围合并
                List<FileRange> nextRange = Collections.singletonList(new FileRange(readPointer, Long.MAX_VALUE));
                List<FileRange> newRanges = bytesRangeService.intersect(Arrays.asList(ranges, nextRange));
                // 合并后范围为空，则移除该范围
                if (newRanges.isEmpty()) {
                    iterator.remove();
                } else {
                    context.queryRanges.put(file, newRanges);
                }
                // 如果不需更多结果，则中断
                if (!needMore) {
                    return false;
                }
            }
        } finally {
            // 释放加载器持有器
            for (LogPackLoader loader : loaderMapForUid.values()) {
                BdFileUtils.closeStream(loader);
            }
        }
        return true;
    }

    /**
     * 是否需要继续添加记录
     */
    private boolean queryByUnfilteredUidSet(QueryResult result, QueryContext context, List<LogPack> logPacks, Map<File, LogPackLoader> loaderMap) throws IOException {
        boolean needMore;
        Iterator<String> iterator = context.unfilteredUidSet.iterator();
        while (iterator.hasNext()) {
            String uid = iterator.next();
            iterator.remove();
            needMore = addResultsByUid(result, context, uid, logPacks, loaderMap);
            if (!needMore) {
                return false;
            }
        }
        return true;
    }

    private boolean addResultsByUid(QueryResult result, QueryContext context, String uid, List<LogPack> formalLogPacks, Map<File, LogPackLoader> loaderMap) throws IOException {
        if (context.usedUidSet.contains(uid)) {
            return true;
        }
        context.usedUidSet.add(uid);
        List<LogPack> packs = getFilteredLogPacksByUid(context, uid, loaderMap);
        context.unusedFilteredResults.addAll(packs);
        return queryByUnusedResults(result, context, formalLogPacks);
    }

    @NonNull
    private List<LogPack> getFilteredLogPacksByUid(QueryContext context, String uid, Map<File, LogPackLoader> loaderMap) throws IOException {
        List<LogPack> packs = uidToLogPacks(context, uid, loaderMap);
        for (LogPack pack : packs) {
            if (!isFiltered(context, pack)) {
                return packs;
            }
        }
        return Collections.emptyList();
    }

    @NonNull
    private List<LogPack> uidToLogPacks(QueryContext context, String uid, Map<File, LogPackLoader> loaderMap) throws IOException {
        List<LogPack> result = new LinkedList<>();
        Map<String, LogPack> uidMap = new HashMap<>();
        // 收集有结束标签的记录
        for (Map.Entry<File, LogIndexes> entry : context.indexesMap.entrySet()) {
            File file = entry.getKey();
            LogIndexes indexes = entry.getValue();
            LogPackLoader loader = getLoader(loaderMap, file, uidMap);
            List<FileRange> ranges = indexes.uidRanges.get(uid);
            if (null == ranges) {
                continue;
            }
            ((RangesLogLineLoader) loader.getLogLineLoader()).switchRanges(ranges);
            LogPack logPack;
            while (null != (logPack = loader.loadNextCompleteLogPack())) {
                if (uid.equals(logPack.uid)) {
                    result.add(logPack);
                }
            }
        }
        // 收集无结束标签的记录
        for (LogPack logPack : uidMap.values()) {
            if (uid.equals(logPack.uid)) {
                result.add(logPack);
            }
        }
        return result;
    }

    private boolean queryByUnusedResults(QueryResult result, QueryContext context, List<LogPack> formalLogPacks) {
        LogPack pack;
        boolean needMore = true;
        while (needMore && null != (pack = context.unusedFilteredResults.poll())) {
            needMore = addToResults(result, context, formalLogPacks, pack);
        }
        return needMore;
    }

    @NonNull
    private LogPackLoader getLoader(Map<File, LogPackLoader> loaderMap, File file, Map<String, LogPack> uidMap) throws IOException {
        LogPackLoader loader = loaderMap.get(file);
        if (null == loader) {
            RangesLogLineLoader lineLoader = new RangesLogLineLoader(file, appConfig.logCharset,
                    appConfig.lineParsePattern, appConfig.tagParsePattern);
            loader = new LogPackLoader(lineLoader, appConfig.maxLinesPerResultWithNullUid, uidMap);
            loaderMap.put(file, loader);
        } else {
            loader.switchUidMap(uidMap);
        }
        return loader;
    }

    private boolean addToResults(QueryResult result, QueryContext context, List<LogPack> formalLogPacks, LogPack candidate) {
        // 添加到结果列表
        formalLogPacks.add(candidate);
        // 是否已达要求的结果数
        boolean needMore = (context.queryParam.getCountLimit() > formalLogPacks.size());
        if (!needMore) {
            result.endReason = "已找到指定数目的结果";
        }
        return needMore;
    }

    /**
     * @return 是否需要更多结果
     */
    private boolean filterAndAddToResults(QueryResult result, QueryContext context, List<LogPack> formalLogPacks, LogPack candidate) {
        // 筛选
        if (isFiltered(context, candidate)) {
            return true;
        }
        return addToResults(result, context, formalLogPacks, candidate);
    }

    private boolean isFiltered(QueryContext context, LogPack logPack) {
        for (LogFilter filter : context.filters) {
            if (filter.filterLogPack(logPack)) {
                return true;
            }
        }
        return false;
    }

    private String exportLogs(QueryResult result, QueryContext context, List<LogPack> formalLogPacks) {
        // 按需分页
        setPageable(result, context);
        // 日志排序
        for (LogPack logPack : formalLogPacks) {
            Collections.sort(logPack.logLines);
        }
        // 导出日志
        return logExporter.export(result, formalLogPacks);
    }

    private FileRange getTimeRange(LogIndexes indexes, String fromTime, String toTime) {
        TreeMap<String, Long> timeIndexMap = indexes.timeIndexMap;
        long startByte = Optional.ofNullable(timeIndexMap.floorEntry(fromTime))
                .map(Map.Entry::getValue).orElse(0L);
        long endByte = Optional.ofNullable(timeIndexMap.ceilingEntry(toTime))
                .map(Map.Entry::getValue).orElse(indexes.scannedBytes);
        return new FileRange(startByte, endByte);
    }

    private void setPageable(QueryResult result, QueryContext context) {
        // 若查询范围、uid映射、临时列表均为空，则不需要分页
        if (context.unusedFilteredResults.isEmpty()
                && context.unfilteredUidSet.isEmpty()
                && context.queryRanges.isEmpty()
                && context.uidTempMap.isEmpty()) {
            return;
        }
        // 创建并绑定下一分页
        QueryResult nextResult = new QueryResult(context);
        result.nextId = nextResult.id;
        nextResult.lastId = result.id;
        queryResultService.registerResult(nextResult);
    }

}
