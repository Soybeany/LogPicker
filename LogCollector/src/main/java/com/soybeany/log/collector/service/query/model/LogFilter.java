package com.soybeany.log.collector.service.query.model;

import com.soybeany.log.core.model.LogPack;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface LogFilter {

    /**
     * 对查询到的logPack进行过滤
     *
     * @return true则过滤指定的logPack
     */
    boolean filterLogPack(LogPack logPack);

}
