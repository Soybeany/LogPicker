package com.soybeany.log.collector.common;

import com.soybeany.log.collector.LogCollector;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.collector.query.data.FileParam;
import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.collector.query.exporter.BaseLogExporter;
import com.soybeany.log.collector.query.provider.DayBasedRollingFileProvider;
import com.soybeany.log.collector.query.provider.FileProvider;
import com.soybeany.log.collector.query.service.QueryService;
import com.soybeany.log.core.model.LogException;
import com.soybeany.util.cache.IDataHolder;
import com.soybeany.util.cache.StdMemDataHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Soybeany
 * @date 2023/2/10
 */
public class LogQueryService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LogCollectConfig config = new LogCollectConfig(
            "^\\[(?<time>.{17})] \\[(?<level>INFO|WARN|ERROR)] \\[(?<uid>.*?)] \\[(?<thread>.+?)] \\{(?<pos>.+?)}-(?<content>.*)",
            "^TAG-(?<key>.+?)-(?<value>.*)",
            "yy-MM-dd HH:mm:ss"
    ).withTagsToIndex(Arrays.asList("url", "user")).withIndexRetainSec(1800);

    private final FileProvider fileProvider = new DayBasedRollingFileProvider(
            "D:\\EFB_Logs\\sysAudit",
            "sysAudit.log",
            "sysAudit.log.<?yyyy-MM-dd?>.log"
    );

    private final IDataHolder<LogIndexes> indexesHolder = new StdMemDataHolder<>(config.maxFileIndexesRetain);
    private final IDataHolder<QueryResult> resultHolder = new StdMemDataHolder<>(config.maxResultRetain);
    private final QueryService queryService = LogCollector
            .query(config)
            .indexesHolder(indexesHolder)
            .resultHolder(resultHolder)
            .build(fileProvider);

    public <T> T query(Map<String, String[]> param, BaseLogExporter<T> exporter) {
        param = handleTimeOffset(param);
        try {
            return queryService.simpleQueryWithMultiValueParam(param, exporter);
        } catch (Exception e) {
            if (e instanceof LogException) {
                throw new RuntimeException(e.getMessage());
            }
            throw e;
        }
    }

    public void stop() throws IOException {
        indexesHolder.close();
        resultHolder.close();
    }

    // ***********************内部方法****************************

    private Map<String, String[]> handleTimeOffset(Map<String, String[]> param) {
        Optional<String> timeOffsetOpt = getParam(param, "timeOffset");
        if (!timeOffsetOpt.isPresent()) {
            return param;
        }
        int timeOffset = Integer.parseInt(timeOffsetOpt.get());
        Optional<LocalDateTime> fromTimeOpt = getParam(param, "fromTime").map(FileParam::parseTime);
        Optional<LocalDateTime> toTimeOpt = getParam(param, "toTime").map(FileParam::parseTime);
        Map<String, String[]> newParam = new HashMap<>(param);
        if (fromTimeOpt.isPresent()) {
            // 有from，无to：按from向后偏移
            if (!toTimeOpt.isPresent()) {
                newParam.put("toTime", new String[]{fromTimeOpt.get().plusMinutes(timeOffset).format(FORMATTER)});
            }
        } else {
            // 无from，有to：按to向前偏移；无from，无to：按当前时间向前偏移
            LocalDateTime refTime = toTimeOpt.orElseGet(LocalDateTime::now);
            newParam.put("fromTime", new String[]{refTime.minusMinutes(timeOffset).format(FORMATTER)});
        }
        return newParam;
    }

    private Optional<String> getParam(Map<String, String[]> param, String key) {
        String[] arr = param.get(key);
        if (null == arr || arr.length < 1) {
            return Optional.empty();
        }
        return Optional.of(arr[0]);
    }

}
