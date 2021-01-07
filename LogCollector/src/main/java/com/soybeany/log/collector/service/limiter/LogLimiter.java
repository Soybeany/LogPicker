package com.soybeany.log.collector.service.limiter;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.RawLogResult;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/6
 */
public interface LogLimiter {

    String PREFIX = "limiter";

    @NonNull
    default Map<String, String> getParams(QueryContext context) {
        return context.queryParam.getParams(PREFIX);
    }

    /**
     * 此限制器的描述
     */
    String getDesc();

    /**
     * 是否能够添加日志结果
     */
    boolean canAddResult(QueryContext context, RawLogResult result);

}
