package com.soybeany.log.collector.query.data;

import com.soybeany.log.collector.common.LogIndexService;
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

    public static QueryIndexes getNew(LogIndexService service, LogIndexes logIndexes) {
        QueryIndexes indexes = new QueryIndexes();
        merge(service, logIndexes, indexes);
        return indexes;
    }

    private static void merge(LogIndexService service, LogIndexes logIndexes, QueryIndexes indexes) {
        // 提取临时数据
        for (LogPack logPack : logIndexes.uidTempMap.values()) {
            service.indexTagAndUid(indexes.uidRanges, indexes.tagUidMap, logPack, false);
        }
        // 合并正式数据
        logIndexes.tagUidMap.forEach((k, v) -> Optional.ofNullable(indexes.tagUidMap.get(k)).ifPresent(m -> m.forEach(
                (k1, v1) -> v.computeIfAbsent(k1, k2 -> new HashSet<>()).addAll(v1))
        ));
        logIndexes.uidRanges.forEach((k, v) -> Optional.ofNullable(indexes.uidRanges.get(k)).ifPresent(v::addAll));
    }

    private QueryIndexes() {
    }

}
