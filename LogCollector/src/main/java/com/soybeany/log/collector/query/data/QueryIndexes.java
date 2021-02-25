package com.soybeany.log.collector.query.data;

import com.soybeany.log.collector.common.LogIndexService;
import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * @author Soybeany
 * @date 2021/2/22
 */
public class QueryIndexes {

    public final LogIndexes logIndexes;

    private final Map<String, LinkedList<FileRange>> uidRanges = new HashMap<>();
    private final Map<String, Map<String, Set<String>>> tagUidMap = new HashMap<>();

    public static QueryIndexes getNew(LogIndexService service, LogIndexes logIndexes) {
        QueryIndexes indexes = new QueryIndexes(logIndexes);
        for (LogPack logPack : logIndexes.uidTempMap.values()) {
            service.indexTagAndUid(indexes.uidRanges, indexes.tagUidMap, logPack, false);
        }
        return indexes;
    }

    private QueryIndexes(LogIndexes logIndexes) {
        this.logIndexes = logIndexes;
    }

    public Map<String, Set<String>> getUidMap(String tag) {
        Map<String, Set<String>> result = logIndexes.tagUidMap.get(tag);
        if (null != result) {
            return result;
        }
        return tagUidMap.get(tag);
    }

    public boolean containUid(String uid) {
        boolean isContain = logIndexes.uidRanges.containsKey(uid);
        if (isContain) {
            return true;
        }
        return uidRanges.containsKey(uid);
    }

    public LinkedList<FileRange> getRanges(String uid) {
        LinkedList<FileRange> result = logIndexes.uidRanges.get(uid);
        if (null != result) {
            return result;
        }
        return uidRanges.get(uid);
    }

}
