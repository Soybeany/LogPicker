package com.soybeany.log.collector.service.query.processor;

import com.soybeany.log.collector.service.common.RangeService;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.core.model.FileRange;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/28
 */
public interface RangeLimiter extends Preprocessor {

    /**
     * 设置未过滤的uid集合
     */
    @Nullable
    default Set<String> onSetupUnfilteredUidSet(FileRange timeRange, LogIndexes indexes) {
        return null;
    }

    /**
     * 设置查询范围
     */
    @Nullable
    default List<FileRange> onSetupQueryRanges(FileRange timeRange, LogIndexes indexes) {
        return null;
    }

    static void filterUidSetByTimeRange(RangeService rangeService, @Nullable Set<String> uidSet, FileRange timeRange, LogIndexes indexes) {
        if (null == uidSet) {
            return;
        }
        List<FileRange> timeRanges = Collections.singletonList(timeRange);
        Iterator<String> uidIterator = uidSet.iterator();
        while (uidIterator.hasNext()) {
            String uid = uidIterator.next();
            LinkedList<FileRange> uidRanges = indexes.uidRanges.get(uid);
            List<FileRange> intersect = rangeService.intersect(Arrays.asList(uidRanges, timeRanges));
            // 若时间无交集，则移除
            if (intersect.isEmpty()) {
                uidIterator.remove();
            }
        }
    }
}
