package com.soybeany.log.collector.query.service;

import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.collector.common.service.LogIndexService;
import com.soybeany.log.collector.common.service.RangeService;
import com.soybeany.log.collector.query.data.QueryContext;
import com.soybeany.log.collector.query.data.QueryIndexes;
import com.soybeany.log.collector.query.data.QueryParam;
import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.collector.query.factory.ModuleFactory;
import com.soybeany.log.collector.query.processor.LogFilter;
import com.soybeany.log.collector.query.processor.Preprocessor;
import com.soybeany.log.collector.query.processor.RangeLimiter;
import com.soybeany.log.collector.query.provider.FileProvider;
import com.soybeany.log.collector.scan.ScanService;
import com.soybeany.log.core.model.Constants;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.IDataHolder;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.util.TimeUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public class QueryResultService {

    private final LogCollectConfig logCollectConfig;
    private final FileProvider fileProvider;
    private final List<ModuleFactory> moduleFactories;
    private final RangeService rangeService;
    private final LogIndexService logIndexService;
    private final ScanService scanService;
    private final IDataHolder<LogIndexes> indexesHolder;
    private final IDataHolder<QueryResult> resultHolder;

    public QueryResultService(LogCollectConfig logCollectConfig, FileProvider fileProvider,
                              List<ModuleFactory> moduleFactories, RangeService rangeService,
                              IDataHolder<LogIndexes> indexesHolder, IDataHolder<QueryResult> resultHolder) {
        this.logCollectConfig = logCollectConfig;
        this.fileProvider = fileProvider;
        this.moduleFactories = moduleFactories;
        this.rangeService = rangeService;
        this.logIndexService = new LogIndexService(logCollectConfig, rangeService);
        this.scanService = new ScanService(logCollectConfig);
        this.indexesHolder = indexesHolder;
        this.resultHolder = resultHolder;
    }

    // ****************************************公开方法****************************************

    public synchronized void registerResult(QueryResult result) {
        resultHolder.put(result.id, result, logCollectConfig.resultRetainSec);
    }

    public QueryResult getResult(Map<String, String> param) {
        QueryResult result = loadResultFromParam(param);
        // 若已有result，则直接使用
        if (null != result) {
            return result;
        }
        // 若还没有，则创建新result
        return getNewResult(param);
    }

    // ****************************************内部方法****************************************

    private QueryResult loadResultFromParam(Map<String, String> param) {
        String resultId = param.get(Constants.PARAM_RESULT_ID);
        if (null == resultId) {
            return null;
        }
        // 若指定了resultId，则尝试获取指定的result
        QueryResult result = resultHolder.updateAndGet(resultId);
        if (null == result) {
            throw new LogException("指定的resultId不存在或已过期");
        }
        return result;
    }

    private QueryResult getNewResult(Map<String, String> param) {
        QueryParam queryParam = new QueryParam(logCollectConfig, param);
        QueryContext context = new QueryContext(queryParam);
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
        Map<File, QueryIndexes> indexesMap = new LinkedHashMap<>();
        for (File logFile : fileProvider.onGetFiles(queryParam)) {
            QueryIndexes indexes = initContextWithFile(queryParam, context, limiters, logFile);
            indexesMap.put(logFile, indexes);
        }
        QueryResult result = new QueryResult(context, indexesMap);
        registerResult(result);
        return result;
    }

    private QueryIndexes initContextWithFile(QueryParam queryParam, QueryContext context, List<RangeLimiter> limiters, File logFile) {
        // 更新索引，并创建查询索引
        LogIndexes indexes = scanService.updateAndGet(context.msgList::add, indexesHolder, logFile);
        QueryIndexes queryIndexes = QueryIndexes.getNew(logIndexService, indexes);
        context.indexesMap.put(logFile, indexes);
        // 设置待查询的范围
        FileRange timeRange = getTimeRange(indexes, queryParam.getFromTime(), queryParam.getToTime());
        List<List<FileRange>> rangeList = new LinkedList<>();
        rangeList.add(Collections.singletonList(timeRange));
        boolean[] shouldInitUidSet = {true};
        for (RangeLimiter limiter : limiters) {
            Optional.ofNullable(limiter.onSetupUnfilteredUidSet(timeRange, queryIndexes)).ifPresent(set -> {
                if (shouldInitUidSet[0]) {
                    context.unfilteredUidSet.addAll(set);
                    shouldInitUidSet[0] = false;
                } else {
                    context.unfilteredUidSet.retainAll(set);
                }
            });
            Optional.ofNullable(limiter.onSetupQueryRanges(timeRange, queryIndexes)).ifPresent(rangeList::add);
        }
        // 合并范围并保存到context
        List<FileRange> intersectedRanges = rangeService.intersect(rangeList);
        if (!intersectedRanges.isEmpty()) {
            context.queryRanges.put(logFile, intersectedRanges);
        }
        return queryIndexes;
    }

    private FileRange getTimeRange(LogIndexes indexes, LocalDateTime fromTime, LocalDateTime toTime) {
        TreeMap<Long, Long> timeIndexMap = indexes.timeIndexMap;
        if (timeIndexMap.isEmpty()) {
            return FileRange.EMPTY;
        }
        // 是否为极端位置
        Long firstTime = timeIndexMap.firstKey();
        Long lastTime = timeIndexMap.lastKey();
        Long fTime = TimeUtils.toMillis(fromTime);
        Long tTime = TimeUtils.toMillis(toTime);
        if (tTime.compareTo(firstTime) < 0 || fTime.compareTo(lastTime) > 0) {
            return FileRange.EMPTY;
        }
        // 正常合并
        long startByte = Optional.ofNullable(timeIndexMap.ceilingEntry(fTime))
                .map(Map.Entry::getValue).orElseThrow(() -> new LogException("不可能的开始时间"));
        long endByte = Optional.ofNullable(timeIndexMap.ceilingEntry(tTime))
                .map(Map.Entry::getValue).orElse(indexes.scannedBytes);
        return new FileRange(startByte, endByte);
    }

}