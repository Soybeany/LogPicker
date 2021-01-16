package com.soybeany.log.collector.service.query.limiter;

import com.soybeany.log.collector.service.query.model.QueryContext;
import com.soybeany.log.core.model.LogPack;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Soybeany
 * @date 2021/1/6
 */
@Component
class CountLogLimiter implements LogLimiter {

    private static final String T_KEY_COUNT = "count";

    @Override
    public String getDesc() {
        return "数目限制";
    }

    @Override
    public boolean canAddResult(QueryContext context, LogPack result) {
        int count = Optional.ofNullable((Integer) context.getTempData(PREFIX, T_KEY_COUNT)).orElse(0);
        if (++count > context.queryParam.getCountLimit()) {
            return false;
        }
        context.putTempData(PREFIX, T_KEY_COUNT, count);
        return true;
    }

}
