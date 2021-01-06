package com.soybeany.log.collector.service.limiter;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.core.model.LogResult;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Soybeany
 * @date 2021/1/6
 */
@Component
public class CountLogLimiter implements LogLimiter {

    private static final String T_KEY_COUNT = "count";

    @Override
    public String getDesc() {
        return "数目限制";
    }

    @Override
    public boolean shouldAddResult(QueryContext context, LogResult result) {
        int count = Optional.ofNullable((Integer) context.getTempData(LIMITER_PREFIX, T_KEY_COUNT)).orElse(0);
        if (++count > context.queryParam.getCountLimit()) {
            return false;
        }
        context.putTempData(LIMITER_PREFIX, T_KEY_COUNT, count);
        return true;
    }

}
