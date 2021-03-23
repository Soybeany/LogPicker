package com.soybeany.log.collector.common.service;

import com.soybeany.config.BDCipherUtils;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.collector.common.model.MsgRecorder;
import com.soybeany.log.collector.common.model.loader.ILogLineLoader;
import com.soybeany.log.collector.common.model.loader.LogPackLoader;
import com.soybeany.log.collector.common.model.loader.SimpleLogLineLoader;
import com.soybeany.log.core.model.*;
import com.soybeany.log.core.util.TimeUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/14
 */
public class LogIndexService {

    private final LogCollectConfig logCollectConfig;
    private final RangeService rangeService;

    public LogIndexService(LogCollectConfig logCollectConfig, RangeService rangeService) {
        this.logCollectConfig = logCollectConfig;
        this.rangeService = rangeService;
    }

    public LogIndexes updateAndGetIndexes(MsgRecorder recorder, IDataHolder<LogIndexes> indexesHolder, File file) throws IOException {
        // 得到索引
        LogIndexes indexes = getIndexes(recorder, indexesHolder, file);
        long startByte = indexes.scannedBytes;
        // 若索引已是最新，则不再更新
        if (file.length() == startByte) {
            recorder.write("“" + file.getName() + "”的索引不需更新(" + startByte + ")");
            return indexes;
        }
        // 更新索引
        long startTime = System.currentTimeMillis();
        try (SimpleLogLineLoader lineLoader = new SimpleLogLineLoader(indexes.logFile, logCollectConfig.logCharset, logCollectConfig.lineParsePattern, logCollectConfig.tagParsePattern, logCollectConfig.lineTimeFormatter);
             LogPackLoader<ILogLineLoader> packLoader = new LogPackLoader<>(lineLoader, logCollectConfig.noUidPlaceholder, logCollectConfig.maxLinesPerResultWithNoUid, indexes.uidTempMap)) {
            lineLoader.resetTo(startByte, null); // 因为不会有旧数据，理论上这里不会null异常
            packLoader.setListener(holder -> indexTime(indexes, holder.fromByte, holder.logLine));
            LogPack logPack;
            while (null != (logPack = packLoader.loadNextCompleteLogPack())) {
                indexTagAndUid(indexes.uidRanges, indexes.tagUidMap, logPack, true);
            }
            indexes.scannedBytes = lineLoader.getReadPointer();
        }
        long spendTime = System.currentTimeMillis() - startTime;
        recorder.write("“" + file.getName() + "”的索引已更新(" + startByte + "~" + indexes.scannedBytes + ")，耗时" + spendTime + "ms");
        return indexes;
    }

    public Map<String, String> getTreatedTagMap(Map<String, String> tags) {
        Map<String, String> result = new HashMap<>();
        // 将tag的值转成小写
        tags.forEach((k, v) -> result.put(k, v.toLowerCase()));
        return result;
    }

    public List<Map.Entry<String, String>> getTreatedTagList(List<LogTag> tags) {
        List<Map.Entry<String, String>> result = new LinkedList<>();
        // 将tag的值转成小写
        tags.forEach(tag -> result.add(new AbstractMap.SimpleEntry<>(tag.key, tag.value.toLowerCase())));
        return result;
    }

    // ********************内部方法********************

    private LogIndexes getIndexes(MsgRecorder recorder, IDataHolder<LogIndexes> indexesHolder, File file) {
        String indexKey = getIndexKey(file);
        LogIndexes indexes = indexesHolder.updateAndGet(indexKey);
        try {
            if (null != indexes) {
                return indexes.withCheck(logCollectConfig);
            }
        } catch (LogException e) {
            recorder.write("重新创建“" + file.getName() + "”的索引文件(" + e.getMessage() + ")");
        }
        indexes = new LogIndexes(logCollectConfig, file);
        indexesHolder.put(indexKey, indexes, logCollectConfig.indexRetainSec);
        return indexes;
    }

    public void indexTagAndUid(Map<String, LinkedList<FileRange>> uidRanges, Map<String, Map<String, Set<String>>> tagUidMap, LogPack logPack, boolean append) {
        for (LogTag tag : logPack.tags) {
            indexTag(tagUidMap, tag);
        }
        indexUid(uidRanges, logPack, append);
    }

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

    private String getIndexKey(File logFile) {
        try {
            return BDCipherUtils.calculateMd5(logFile.getAbsolutePath());
        } catch (Exception e) {
            throw new LogException(e);
        }
    }

}
