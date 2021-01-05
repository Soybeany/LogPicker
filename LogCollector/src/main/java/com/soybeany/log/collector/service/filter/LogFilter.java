package com.soybeany.log.collector.service.filter;

import com.soybeany.log.collector.model.Context;
import com.soybeany.log.collector.model.LogSection;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface LogFilter extends Comparable<LogFilter> {

    String FILTER_PREFIX = "filter";

    @Override
    default int compareTo(LogFilter o) {
        return o.priority() - priority();
    }

    /**
     * 是否应该将指定的section过滤掉
     *
     * @return true则过滤指定的section
     */
    boolean shouldFilter(Context context, LogSection section);

    @NonNull
    default Map<String, String> getParams(Context context) {
        return context.param.getParams(FILTER_PREFIX);
    }

    /**
     * 过滤器的优先级，值越大，越先被执行，默认值为0
     */
    default int priority() {
        return 0;
    }

}
