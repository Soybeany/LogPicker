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
public class MaxBytesReturnLogLimiter implements LogLimiter, QueryContext.IListener {

    private static final String P_KEY_MAX_BYTES_RETURN = "maxBytesReturn";
    private static final String T_KEY_MAX_BYTES_RETURN = "maxBytesReturn";
    private static final String T_KEY_BYTES = "bytes";

    private static final long DEFAULT_MAX_BYTES_RETURN = 10000000;

    @Override
    public String getDesc() {
        return "最大返回字节数限制";
    }

    @Override
    public void onInitTempData(QueryContext context) {
        String limit = context.queryParam.getParams(LIMITER_PREFIX).get(P_KEY_MAX_BYTES_RETURN);
        long bytes = (null != limit ? Long.parseLong(limit) : DEFAULT_MAX_BYTES_RETURN);
        context.putTempData(LIMITER_PREFIX, T_KEY_MAX_BYTES_RETURN, bytes);
    }

    @Override
    public boolean shouldAddResult(QueryContext context, LogResult result) {
        long bytes = Optional.ofNullable((Long) context.getTempData(LIMITER_PREFIX, T_KEY_BYTES)).orElse(0L);
        // 计算待评估结果的字节数
        long[] totalBytes = new long[1];
        result.sections.forEach(section -> section.logs.forEach(log -> {
            totalBytes[0] += log.getBytes().length;
        }));
        if (bytes + totalBytes[0] > getBytesLimit(context)) {
            return false;
        }
        context.putTempData(LIMITER_PREFIX, T_KEY_BYTES, totalBytes[0]);
        return true;
    }

    private long getBytesLimit(QueryContext context) {
        return context.getTempData(LIMITER_PREFIX, T_KEY_MAX_BYTES_RETURN);
    }

}
