package com.soybeany.log.collector.service.filter;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.core.model.LogSection;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface LogFilter {

    String FILTER_PREFIX = "filter";

    @NonNull
    default Map<String, String> getParams(QueryContext context) {
        return context.queryParam.getParams(FILTER_PREFIX);
    }

    /**
     * 是否应该将指定的section过滤掉
     *
     * @return true则过滤指定的section
     */
    boolean shouldFilter(QueryContext context, LogSection section);

}
