package com.soybeany.log.collector.query.service;

import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.collector.common.model.loader.LogPackLoader;
import com.soybeany.log.collector.common.model.loader.RangesLogLineLoader;
import com.soybeany.log.collector.common.service.RangeService;
import com.soybeany.log.collector.query.data.QueryContext;
import com.soybeany.log.collector.query.data.QueryIndexes;
import com.soybeany.log.collector.query.data.QueryParam;
import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.collector.query.exporter.LogExporter;
import com.soybeany.log.collector.query.factory.ModuleFactory;
import com.soybeany.log.collector.query.processor.LogFilter;
import com.soybeany.log.collector.query.provider.FileProvider;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.util.cache.IDataHolder;
import com.soybeany.util.file.BdFileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class QueryService implements Closeable {

    private static final ThreadLocal<Long> READ_BYTES_LOCAL = new ThreadLocal<>();

    private final LogCollectConfig logCollectConfig;
    private final QueryResultService queryResultService;
    private final RangeService rangeService;

    public QueryService(LogCollectConfig logCollectConfig, FileProvider fileProvider, List<ModuleFactory> moduleFactories,
                        IDataHolder<LogIndexes> indexesHolder, IDataHolder<QueryResult> resultHolder) {
        this.logCollectConfig = logCollectConfig;
        this.rangeService = new RangeService(logCollectConfig);
        this.queryResultService = new QueryResultService(logCollectConfig, fileProvider, moduleFactories, rangeService, indexesHolder, resultHolder);
    }

    @Override
    public void close() throws IOException {
        queryResultService.close();
    }

    public <T> T simpleQuery(Map<String, String> param, LogExporter<T> logExporter) {
        return simpleQueryWithMultiValueParam(QueryParam.toMultiValueMap(param), logExporter);
    }

    public <T> T simpleQueryWithMultiValueParam(Map<String, String[]> param, LogExporter<T> logExporter) {
        QueryResult result = queryResultService.getResult(param);
        try {
            result.lock();
            if (null == result.logPacks) {
                try {
                    READ_BYTES_LOCAL.set(0L);
                    result.startTimeRecord();
                    result.logPacks = queryLogPacks(result);
                } finally {
                    result.stopTimeRecord();
                    READ_BYTES_LOCAL.remove();
                }
            }
        } catch (Exception e) {
            throw new LogException(e);
        } finally {
            result.unlock();
        }
        return logExporter.export(result);
    }

    // ********************内部方法********************

    private List<LogPack> queryLogPacks(QueryResult result) throws IOException {
        List<LogPack> formalLogPacks = new LinkedList<>();
        boolean needMore;
        // 如果有未使用的结果，则直接使用
        needMore = popUnusedResults(result, formalLogPacks);
        // 创建可复用的加载器持有器，遇到相同file时不需重新创建
        Map<File, LogPackLoader<RangesLogLineLoader>> loaderMap = new HashMap<>();
        try {
            if (needMore) {
                needMore = queryByUnfilteredUidSet(result, formalLogPacks, loaderMap);
            }
            if (needMore) {
                Map<File, LogPackLoader<RangesLogLineLoader>> loaderMapForUid = new HashMap<>();
                try {
                    needMore = queryByRanges(result, formalLogPacks, loaderMap, loaderMapForUid);
                } finally {
                    // 释放加载器持有器
                    for (LogPackLoader<RangesLogLineLoader> loader : loaderMapForUid.values()) {
                        BdFileUtils.closeStream(loader);
                    }
                }
            }
            // 检测文件是否有变更
            result.context.indexesMap.values().forEach(indexes -> indexes.check(logCollectConfig));
        } finally {
            // 释放加载器持有器
            for (LogPackLoader<RangesLogLineLoader> loader : loaderMap.values()) {
                BdFileUtils.closeStream(loader);
            }
        }
        // 如果记录数不够，则继续遍历临时列表并弹出记录
        if (needMore) {
            popResultsFromUidMap(result, formalLogPacks);
        }
        // 返回结果
        pagingAndSort(result, formalLogPacks);
        return formalLogPacks;
    }

    private void popResultsFromUidMap(QueryResult result, List<LogPack> formalLogPacks) {
        Iterator<LogPack> iterator = result.context.uidTempMap.values().iterator();
        while (iterator.hasNext()) {
            LogPack logPack = iterator.next();
            iterator.remove();
            // 若已使用，或是被过滤，则继续遍历
            if (result.context.returnedUidSet.contains(logPack.uid)
                    || isFiltered(result.context, logPack)) {
                continue;
            }
            boolean needMore = addToFormalLogPacks(result, formalLogPacks, logPack);
            if (!needMore) {
                return;
            }
        }
    }

    private boolean queryByRanges(QueryResult result, List<LogPack> formalLogPacks, Map<File, LogPackLoader<RangesLogLineLoader>> loaderMap, Map<File, LogPackLoader<RangesLogLineLoader>> loaderMapForUid) throws IOException {
        boolean needMore = true;
        Iterator<Map.Entry<File, List<FileRange>>> iterator = result.context.queryRanges.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<File, List<FileRange>> entry = iterator.next();
            File file = entry.getKey();
            List<FileRange> ranges = entry.getValue();
            LogPackLoader<RangesLogLineLoader> loader = getLoader(loaderMap, file, result.context.uidTempMap, ranges);
            LogPack logPack;
            while (needMore && null != (logPack = loader.loadNextCompleteLogPack())) {
                if (isFiltered(result.context, logPack)) {
                    continue;
                }
                if (!logCollectConfig.noUidPlaceholder.equals(logPack.uid)) {
                    needMore = addResultsByUid(result, logPack.uid, formalLogPacks, loaderMapForUid, false);
                } else {
                    needMore = addToFormalLogPacks(result, formalLogPacks, logPack);
                }
            }
            RangesLogLineLoader rangesLogLineLoader = loader.getLogLineLoader();
            READ_BYTES_LOCAL.set(READ_BYTES_LOCAL.get() + rangesLogLineLoader.getReadBytes());
            long readPointer = rangesLogLineLoader.getReadPointer();
            // 使用已查询到的字节，再次进行范围合并
            List<FileRange> nextRange = Collections.singletonList(new FileRange(readPointer, Long.MAX_VALUE));
            List<FileRange> newRanges = rangeService.intersect(Arrays.asList(ranges, nextRange));
            // 合并后范围为空，则移除该范围
            if (newRanges.isEmpty()) {
                iterator.remove();
            } else {
                result.context.queryRanges.put(file, newRanges);
            }
            // 如果不需更多结果，则中断
            if (!needMore) {
                break;
            }
        }
        return needMore;
    }

    /**
     * 是否需要继续添加记录
     */
    private boolean queryByUnfilteredUidSet(QueryResult result, List<LogPack> logPacks, Map<File, LogPackLoader<RangesLogLineLoader>> loaderMap) throws IOException {
        boolean needMore = true;
        Iterator<String> iterator = result.context.unfilteredUidSet.iterator();
        while (iterator.hasNext()) {
            String uid = iterator.next();
            iterator.remove();
            needMore = addResultsByUid(result, uid, logPacks, loaderMap, true);
            if (!needMore) {
                break;
            }
        }
        return needMore;
    }

    private boolean addResultsByUid(QueryResult result, String uid, List<LogPack> formalLogPacks, Map<File, LogPackLoader<RangesLogLineLoader>> loaderMap, boolean needFilter) throws IOException {
        QueryContext context = result.context;
        if (context.returnedUidSet.contains(uid)) {
            return true;
        }
        context.returnedUidSet.add(uid);
        List<LogPack> packs = getFilteredLogPacksByUid(result, uid, loaderMap, needFilter);
        context.unusedFilteredResults.addAll(packs);
        return popUnusedResults(result, formalLogPacks);
    }

    private List<LogPack> getFilteredLogPacksByUid(QueryResult queryResult, String uid, Map<File, LogPackLoader<RangesLogLineLoader>> loaderMap, boolean needFilter) throws IOException {
        List<LogPack> packs = uidToLogPacks(queryResult, uid, loaderMap);
        if (!needFilter) {
            return packs;
        }
        for (LogPack pack : packs) {
            if (!isFiltered(queryResult.context, pack)) {
                return packs;
            }
        }
        return Collections.emptyList();
    }

    private List<LogPack> uidToLogPacks(QueryResult queryResult, String uid, Map<File, LogPackLoader<RangesLogLineLoader>> loaderMap) throws IOException {
        List<LogPack> result = new LinkedList<>();
        Map<String, LogPack> uidMap = new HashMap<>();
        // 收集有结束标签的记录
        for (Map.Entry<File, QueryIndexes> entry : queryResult.indexesMap.entrySet()) {
            File file = entry.getKey();
            QueryIndexes indexes = entry.getValue();
            List<FileRange> ranges = indexes.getMergedRanges(rangeService, uid);
            if (ranges.isEmpty()) {
                continue;
            }
            LogPackLoader<RangesLogLineLoader> loader = getLoader(loaderMap, file, uidMap, ranges);
            LogPack logPack;
            while (null != (logPack = loader.loadNextCompleteLogPack())) {
                if (uid.equals(logPack.uid)) {
                    result.add(logPack);
                }
            }
            RangesLogLineLoader rangesLogLineLoader = loader.getLogLineLoader();
            READ_BYTES_LOCAL.set(READ_BYTES_LOCAL.get() + rangesLogLineLoader.getReadBytes());
        }
        // 收集无结束标签的记录
        for (LogPack logPack : uidMap.values()) {
            if (uid.equals(logPack.uid)) {
                result.add(logPack);
            }
        }
        return result;
    }

    private boolean popUnusedResults(QueryResult result, List<LogPack> formalLogPacks) {
        LogPack pack;
        boolean needMore = true;
        while (needMore && null != (pack = result.context.unusedFilteredResults.poll())) {
            needMore = addToFormalLogPacks(result, formalLogPacks, pack);
        }
        return needMore;
    }

    private LogPackLoader<RangesLogLineLoader> getLoader(Map<File, LogPackLoader<RangesLogLineLoader>> loaderMap, File file, Map<String, LogPack> uidMap, List<FileRange> ranges) throws IOException {
        LogPackLoader<RangesLogLineLoader> loader = loaderMap.get(file);
        if (null == loader) {
            RangesLogLineLoader lineLoader = new RangesLogLineLoader(file, logCollectConfig.logCharset,
                    logCollectConfig.lineParsePattern, logCollectConfig.tagParsePattern, logCollectConfig.lineTimeFormatter);
            loader = new LogPackLoader<>(lineLoader, logCollectConfig.noUidPlaceholder, logCollectConfig.maxLinesPerResultWithNoUid, uidMap);
            loaderMap.put(file, loader);
        } else {
            loader.switchUidMap(uidMap);
        }
        RangesLogLineLoader rangesLogLineLoader = loader.getLogLineLoader();
        rangesLogLineLoader.switchRanges(ranges);
        return loader;
    }

    private boolean addToFormalLogPacks(QueryResult result, List<LogPack> formalLogPacks, LogPack candidate) {
        // 添加到结果列表
        formalLogPacks.add(candidate);
        // 是否已达要求的结果数
        boolean needMore = (result.context.queryParam.getCountLimit() > formalLogPacks.size());
        if (!needMore) {
            result.endReason = "已找到指定数目的结果";
        }
        return needMore;
    }

    private boolean isFiltered(QueryContext context, LogPack logPack) {
        for (LogFilter filter : context.filters) {
            if (filter.filterLogPack(logPack)) {
                return true;
            }
        }
        return false;
    }

    private void pagingAndSort(QueryResult result, List<LogPack> formalLogPacks) {
        // 按需分页
        setPageable(result);
        // 日志排序
        for (LogPack logPack : formalLogPacks) {
            Collections.sort(logPack.logLines);
        }
        // 设置结果
        result.msgList.add("总查询字节数:" + READ_BYTES_LOCAL.get());
        result.msgList.add("结果条数:" + formalLogPacks.size() + "(max:" + result.context.queryParam.getCountLimit() + ")");
    }

    private void setPageable(QueryResult result) {
        // 若查询范围、uid映射、临时列表均为空，则不需要分页
        if (result.context.unusedFilteredResults.isEmpty()
                && result.context.unfilteredUidSet.isEmpty()
                && result.context.queryRanges.isEmpty()
                && result.context.uidTempMap.isEmpty()) {
            return;
        }
        // 创建并绑定下一分页
        QueryResult nextResult = new QueryResult(result.context, result.indexesMap);
        result.nextId = nextResult.id;
        nextResult.lastId = result.id;
        queryResultService.registerResult(nextResult);
    }
}
