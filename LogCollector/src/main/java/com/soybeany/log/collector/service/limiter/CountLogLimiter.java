package com.soybeany.log.collector.service.limiter;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.RawLogResult;
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
    public boolean canAddResult(QueryContext context, RawLogResult result) {
        int count = Optional.ofNullable((Integer) context.getTempData(PREFIX, T_KEY_COUNT)).orElse(0);
        if (++count > context.queryParam.getCountLimit()) {
            return false;
        }
        context.putTempData(PREFIX, T_KEY_COUNT, count);
        return true;
    }

}
