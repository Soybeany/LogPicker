package com.soybeany.log.collector.query.data;

import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.collector.common.service.LogIndexService;
import com.soybeany.log.collector.common.service.RangeService;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * 查询用的索引，因每次查询都会发生变化，所以与结果绑定
 *
 * @author Soybeany
 * @date 2021/2/22
 */
public class QueryIndexes {

    private final Map<String, LinkedList<FileRange>> uidRanges = new HashMap<>();
    private final Map<String, Map<String, Set<String>>> tagUidMap = new HashMap<>();
    private final LogIndexes logIndexes;

    public static QueryIndexes getNew(LogIndexService indexService, LogIndexes logIndexes) {
        QueryIndexes indexes = new QueryIndexes(logIndexes);
        // 提取临时数据
        for (LogPack logPack : logIndexes.uidTempMap.values()) {
            indexService.indexTagAndUid(indexes.uidRanges, indexes.tagUidMap, logPack, false);
        }
        return indexes;
    }

    private QueryIndexes(LogIndexes logIndexes) {
        this.logIndexes = logIndexes;
    }

    public void forEach(String tagKey, BiConsumer<String, Set<String>> consumer) {
        forEach(logIndexes.tagUidMap, tagKey, consumer);
        forEach(tagUidMap, tagKey, consumer);
    }

    public boolean containUid(String uid) {
        boolean isContain = logIndexes.uidRanges.containsKey(uid);
        if (isContain) {
            return true;
        }
        return uidRanges.containsKey(uid);
    }

    public LinkedList<FileRange> getMergedRanges(RangeService service, String uid) {
        LinkedList<FileRange> ranges = getRanges(uid);
        return service.merge(ranges);
    }

    public LinkedList<FileRange> getRanges(String uid) {
        LinkedList<FileRange> result = new LinkedList<>();
        Optional.ofNullable(logIndexes.uidRanges.get(uid)).ifPresent(result::addAll);
        Optional.ofNullable(uidRanges.get(uid)).ifPresent(result::addAll);
        return result;
    }

    // ********************内部方法********************

    private void forEach(Map<String, Map<String, Set<String>>> tagUidMap, String tagKey, BiConsumer<String, Set<String>> consumer) {
        Map<String, Set<String>> tagValueMap = tagUidMap.get(tagKey);
        if (null == tagValueMap) {
            return;
        }
        tagValueMap.forEach(consumer);
    }

}
