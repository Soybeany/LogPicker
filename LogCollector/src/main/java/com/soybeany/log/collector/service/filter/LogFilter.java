package com.soybeany.log.collector.service.filter;

import com.soybeany.log.collector.model.LogPack;
import com.soybeany.log.collector.model.QueryContext;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface LogFilter {

    String PREFIX = "filter";

    /**
     * 是否应该将指定的result过滤掉
     *
     * @return true则过滤指定的result
     */
    boolean shouldFilter(QueryContext context, LogPack result);

}
