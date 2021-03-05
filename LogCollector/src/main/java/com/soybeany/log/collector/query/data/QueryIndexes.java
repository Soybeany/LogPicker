package com.soybeany.log.collector.query.data;

import com.soybeany.log.collector.common.LogIndexService;
import com.soybeany.log.collector.common.RangeService;
import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/2/22
 */
public class QueryIndexes {

    public final Map<String, LinkedList<FileRange>> uidRanges = new HashMap<>();
    public final Map<String, Map<String, Set<String>>> tagUidMap = new HashMap<>();

    public static QueryIndexes getNew(LogIndexService indexService, RangeService rangeService, LogIndexes logIndexes) {
        QueryIndexes indexes = new QueryIndexes();
        merge(indexService, rangeService, logIndexes, indexes);
        return indexes;
    }

    private static void merge(LogIndexService service, RangeService rangeService, LogIndexes logIndexes, QueryIndexes indexes) {
        // 提取临时数据
        for (LogPack logPack : logIndexes.uidTempMap.values()) {
            service.indexTagAndUid(indexes.uidRanges, indexes.tagUidMap, logPack, false);
        }
        // 合并正式数据
        mergeTagUidMap(logIndexes, indexes);
        mergeUidRanges(rangeService, logIndexes, indexes);
    }

    private static void mergeTagUidMap(LogIndexes logIndexes, QueryIndexes indexes) {
        logIndexes.tagUidMap.forEach((sTagName, sValueMap) -> {
            Map<String, Set<String>> tValueMap = indexes.tagUidMap.get(sTagName);
            if (null == tValueMap) {
                indexes.tagUidMap.put(sTagName, new HashMap<>(sValueMap));
                return;
            }
            sValueMap.forEach((sTagValue, uidSet) -> tValueMap.computeIfAbsent(sTagValue, k -> new HashSet<>()).addAll(uidSet));
        });
    }

    private static void mergeUidRanges(RangeService rangeService, LogIndexes logIndexes, QueryIndexes indexes) {
        logIndexes.uidRanges.forEach((sUid, sRanges) -> {
            LinkedList<FileRange> ranges = indexes.uidRanges.get(sUid);
            if (null == ranges) {
                ranges = new LinkedList<>();
            }
            ranges.addAll(sRanges);
            indexes.uidRanges.put(sUid, rangeService.merge(ranges));
        });
    }

    private QueryIndexes() {
    }

}
