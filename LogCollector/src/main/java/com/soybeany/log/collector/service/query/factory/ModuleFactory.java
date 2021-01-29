package com.soybeany.log.collector.service.query.factory;

import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.collector.service.query.model.LogFilter;
import com.soybeany.log.collector.service.query.model.RangeLimiter;
import org.springframework.lang.Nullable;

/**
 * @author Soybeany
 * @date 2021/1/24
 */
public interface ModuleFactory {

    /**
     * 若有需要，则返回新的范围限制器
     */
    @Nullable
    RangeLimiter getNewRangeLimiterIfInNeed(QueryContext context);

    /**
     * 若有需要，则返回新的日志过滤器
     */
    @Nullable
    LogFilter getNewLogFilterIfInNeed(QueryContext context);

}
