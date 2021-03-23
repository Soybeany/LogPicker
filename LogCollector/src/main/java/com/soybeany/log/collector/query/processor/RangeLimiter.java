package com.soybeany.log.collector.query.processor;

import com.soybeany.log.collector.common.service.RangeService;
import com.soybeany.log.collector.query.data.QueryIndexes;
import com.soybeany.log.core.model.FileRange;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/28
 */
public interface RangeLimiter extends Preprocessor {

    /**
     * 设置未过滤的uid集合
     */
    default Set<String> onSetupUnfilteredUidSet(FileRange timeRange, QueryIndexes indexes) {
        return null;
    }

    /**
     * 设置查询范围
     */
    default List<FileRange> onSetupQueryRanges(FileRange timeRange, QueryIndexes indexes) {
        return null;
    }

    static void filterUidSetByTimeRange(RangeService rangeService, Set<String> uidSet, FileRange timeRange, QueryIndexes indexes) {
        if (null == uidSet) {
            return;
        }
        List<FileRange> timeRanges = Collections.singletonList(timeRange);
        Iterator<String> uidIterator = uidSet.iterator();
        while (uidIterator.hasNext()) {
            String uid = uidIterator.next();
            List<FileRange> uidRanges = indexes.getMergedRanges(rangeService, uid);
            List<FileRange> intersect = rangeService.intersect(Arrays.asList(uidRanges, timeRanges));
            // 若时间无交集，则移除
            if (intersect.isEmpty()) {
                uidIterator.remove();
            }
        }
    }
}
