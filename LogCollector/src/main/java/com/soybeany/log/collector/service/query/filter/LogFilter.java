package com.soybeany.log.collector.service.query.filter;

import com.soybeany.log.collector.service.query.model.QueryContext;
import com.soybeany.log.core.model.LogPack;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface LogFilter {

    String PREFIX = "filter";

    /**
     * 是否应该将指定的logPack过滤掉
     *
     * @return true则过滤指定的logPack
     */
    boolean shouldFilter(QueryContext context, LogPack logPack);

}
