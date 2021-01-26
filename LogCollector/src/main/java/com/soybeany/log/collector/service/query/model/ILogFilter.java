package com.soybeany.log.collector.service.query.model;

import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface ILogFilter {

    List<FileRange> DEFAULT_RANGES = Collections.singletonList(new FileRange(0, Long.MAX_VALUE));

    /**
     * 对查询范围进行过滤
     */
    @NonNull
    default List<FileRange> getFilteredRanges(LogIndexes indexes) {
        return DEFAULT_RANGES;
    }

    /**
     * 对查询到的logPack进行过滤
     *
     * @return true则过滤指定的logPack
     */
    boolean filterLogPack(LogPack logPack);

}
