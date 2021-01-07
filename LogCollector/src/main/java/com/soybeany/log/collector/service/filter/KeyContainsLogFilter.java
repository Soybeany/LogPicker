package com.soybeany.log.collector.service.filter;

import com.soybeany.log.collector.model.LogLine;
import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.RawLogResult;
import org.springframework.stereotype.Component;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
@Component
class KeyContainsLogFilter implements LogFilter {

    private static final String P_KEY_CONTAINS_KEY = "containsKey";

    @Override
    public boolean shouldFilter(QueryContext context, RawLogResult result) {
        // 若没有配置，则不作过滤
        String key = getParams(context).get(P_KEY_CONTAINS_KEY);
        if (null == key) {
            return false;
        }
        // 遍历日志，若包含指定关键词则保留该条记录
        for (LogLine logLine : result.logLines) {
            if (logLine.content.contains(key)) {
                return false;
            }
        }
        // 否则进行过滤
        return true;
    }

}
