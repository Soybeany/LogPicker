package com.soybeany.log.collector.service.limiter;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.model.IQueryListener;
import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.core.model.LogPack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Soybeany
 * @date 2021/1/6
 */
@Component
class MaxBytesReturnLogLimiter implements LogLimiter, IQueryListener {

    private static final String P_KEY_MAX_BYTES_RETURN = "maxBytesReturn";
    private static final String T_KEY_MAX_BYTES_RETURN = "maxBytesReturn";
    private static final String T_KEY_BYTES = "bytes";

    @Autowired
    private AppConfig appConfig;

    @Override
    public String getDesc() {
        return "最大返回字节数限制";
    }

    @Override
    public void onQuery(QueryContext context) {
        String limit = context.getParam(PREFIX, P_KEY_MAX_BYTES_RETURN);
        long bytes = (null != limit ? Long.parseLong(limit) : appConfig.maxBytesReturn);
        context.putTempData(PREFIX, T_KEY_MAX_BYTES_RETURN, bytes);
    }

    @Override
    public boolean canAddResult(QueryContext context, LogPack result) {
        long bytes = Optional.ofNullable((Long) context.getTempData(PREFIX, T_KEY_BYTES)).orElse(0L);
        // 计算待评估结果的字节数
        long[] totalBytes = new long[1];
        result.logLines.forEach(log -> totalBytes[0] += log.content.getBytes().length);
        if (bytes + totalBytes[0] > getBytesLimit(context)) {
            return false;
        }
        context.putTempData(PREFIX, T_KEY_BYTES, totalBytes[0]);
        return true;
    }

    private long getBytesLimit(QueryContext context) {
        return context.getTempData(PREFIX, T_KEY_MAX_BYTES_RETURN);
    }

}
