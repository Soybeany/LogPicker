package com.soybeany.log.collector.service.filter;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.core.model.LogSection;
import org.springframework.stereotype.Component;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
@Component
public class KeyContainsLogFilter implements LogFilter {

    private static final String P_KEY_CONTAINS_KEY = "containsKey";

    @Override
    public boolean shouldFilter(QueryContext context, LogSection section) {
        // 若没有配置，则不作过滤
        String key = getParams(context).get(P_KEY_CONTAINS_KEY);
        if (null == key) {
            return false;
        }
        // 遍历日志，若包含指定关键词则保留该条记录
        for (String log : section.logs) {
            if (log.contains(key)) {
                return false;
            }
        }
        // 否则进行过滤
        return true;
    }

}
