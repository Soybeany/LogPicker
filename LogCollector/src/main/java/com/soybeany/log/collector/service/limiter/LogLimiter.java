package com.soybeany.log.collector.service.limiter;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.core.model.LogResult;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/6
 */
public interface LogLimiter {

    String LIMITER_PREFIX = "limiter";

    @NonNull
    default Map<String, String> getParams(QueryContext context) {
        return context.queryParam.getParams(LIMITER_PREFIX);
    }

    /**
     * 此限制器的描述
     */
    String getDesc();

    /**
     * 是否应该添加日志结果
     */
    boolean shouldAddResult(QueryContext context, LogResult result);

}
