package com.soybeany.log.collector.service.query.filter;

import com.soybeany.log.collector.service.common.model.LogFilter;
import com.soybeany.log.collector.service.query.data.QueryContext;

/**
 * @author Soybeany
 * @date 2021/1/24
 */
public interface LogFilterFactory {

    String PREFIX = "filter";

    /**
     * 若有需要，则返回新的日志过滤器
     */
    LogFilter getNewLogFilterIfInNeed(QueryContext context);

}
