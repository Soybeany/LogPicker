package com.soybeany.log.collector.service.filter;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.RawLogResult;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface LogFilter {

    String PREFIX = "filter";

    @NonNull
    default Map<String, String> getParams(QueryContext context) {
        return context.queryParam.getParams(PREFIX);
    }

    /**
     * 是否应该将指定的result过滤掉
     *
     * @return true则过滤指定的result
     */
    boolean shouldFilter(QueryContext context, RawLogResult result);

}
