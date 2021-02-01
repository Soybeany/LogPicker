package com.soybeany.log.collector.service.query.processor;

import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.core.model.FileRange;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Set;

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

}
