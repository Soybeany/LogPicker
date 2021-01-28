package com.soybeany.log.collector.service.common.model;

import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;
import org.springframework.lang.NonNull;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface LogFilter {

    /**
     * 对设置查询范围
     */
    @NonNull
    default void onSetupRanges(Map<String, Map<File, List<FileRange>>> uidMap, FileRange timeRange, LogIndexes indexes) {
    }

    /**
     * 对查询到的logPack进行过滤
     *
     * @return true则过滤指定的logPack
     */
    boolean filterLogPack(LogPack logPack);

}
