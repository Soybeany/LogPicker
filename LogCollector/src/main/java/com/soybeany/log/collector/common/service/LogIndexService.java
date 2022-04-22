package com.soybeany.log.collector.common.service;

import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.collector.common.model.MsgRecorder;
import com.soybeany.log.collector.common.model.loader.LogLineHolder;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.model.LogTag;
import com.soybeany.log.core.util.TimeUtils;
import com.soybeany.util.cache.IDataHolder;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/14
 */
public class LogIndexService extends BaseScanService<LogIndexes> {

    private final RangeService rangeService;

    // ********************静态方法********************

    public static Map<String, String[]> valueToLowerCase(Map<String, String[]> tags) {
        Map<String, String[]> result = new HashMap<>();
        // 将tag的值转成小写
        tags.forEach((k, arr) -> {
            String[] newArr = new String[arr.length];
            for (int i = 0; i < arr.length; i++) {
                newArr[i] = arr[i].toLowerCase();
            }
            result.put(k, newArr);
        });
        return result;
    }

    public static List<Map.Entry<String, String>> valueToLowerCase(List<LogTag> tags) {
        List<Map.Entry<String, String>> result = new LinkedList<>();
        // 将tag的值转成小写
        tags.forEach(tag -> result.add(new AbstractMap.SimpleEntry<>(tag.key, tag.value.toLowerCase())));
        return result;
    }

    public static String[] valueToLowerCase(String[] values) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].toLowerCase();
        }
        return result;
    }

    public static String valueToLowerCase(String value) {
        return value.toLowerCase();
    }

    // ********************实例方法********************

    public LogIndexService(LogCollectConfig logCollectConfig, RangeService rangeService) {
        super(logCollectConfig);
        this.rangeService = rangeService;
    }

    @Override
    public LogIndexes onGetNewUnit(LogCollectConfig logCollectConfig, File logFile) {
        return new LogIndexes(logCollectConfig, logFile);
    }

    @Override
    public void onHandleLogLine(LogIndexes indexes, LogLineHolder holder) {
        indexTime(indexes, holder.fromByte, holder.logLine);
    }

    @Override
    public void onHandleLogPack(LogIndexes indexes, LogPack logPack) {
        indexTagAndUid(indexes.uidRanges, indexes.tagUidMap, logPack, true);
    }

    public LogIndexes updateAndGetIndexes(MsgRecorder recorder, IDataHolder<LogIndexes> unitHolder, File file) throws IOException {
        return updateAndGetUnit("索引", recorder, unitHolder, file);
    }

    public void indexTagAndUid(Map<String, LinkedList<FileRange>> uidRanges, Map<String, Map<String, Set<String>>> tagUidMap, LogPack logPack, boolean append) {
        for (LogTag tag : logPack.tags) {
            indexTag(tagUidMap, tag);
        }
        indexUid(uidRanges, logPack, append);
    }

    // ********************内部方法********************

    private void indexTag(Map<String, Map<String, Set<String>>> tagUidMap, LogTag logTag) {
        if (!logCollectConfig.tagsToIndex.contains(logTag.key)) {
            return;
        }
        // 将tag的值转成小写，并保存到tagUidMap中
        Set<String> totalUidList = tagUidMap.computeIfAbsent(logTag.key, k -> new HashMap<>())
                .computeIfAbsent(logTag.value.toLowerCase(), k -> new HashSet<>());
        totalUidList.add(logTag.uid);
    }

    private void indexUid(Map<String, LinkedList<FileRange>> uidRanges, LogPack logPack, boolean append) {
        LinkedList<FileRange> totalRanges = uidRanges.computeIfAbsent(logPack.uid, k -> new LinkedList<>());
        if (append) {
            for (FileRange range : logPack.ranges) {
                rangeService.append(totalRanges, range.from, range.to);
            }
        } else {
            totalRanges.addAll(logPack.ranges);
            LinkedList<FileRange> newRanges = rangeService.merge(totalRanges);
            uidRanges.put(logPack.uid, newRanges);
        }
    }

    private void indexTime(LogIndexes indexes, long fromByte, LogLine logLine) {
        indexes.timeIndexMap.putIfAbsent(getModifiedMillis(logLine), fromByte);
    }

    private long getModifiedMillis(LogLine logLine) {
        return TimeUtils.toMillis(logLine.time) / 1000 * 1000;
    }

}
