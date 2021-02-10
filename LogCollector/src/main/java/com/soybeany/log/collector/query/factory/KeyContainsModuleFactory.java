package com.soybeany.log.collector.query.factory;

import com.soybeany.log.collector.query.data.QueryContext;
import com.soybeany.log.collector.query.processor.LogFilter;
import com.soybeany.log.collector.query.processor.Preprocessor;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogPack;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class KeyContainsModuleFactory implements ModuleFactory {

    private static final String P_KEY_CONTAINS_KEY = "containsKey";

    @Override
    public void onSetupPreprocessors(QueryContext context, List<Preprocessor> preprocessors) {
        String key = context.getParam(LogFilter.PREFIX, P_KEY_CONTAINS_KEY);
        // 若没有配置，则不作预处理
        if (null == key) {
            return;
        }
        // 设置新的过滤器
        preprocessors.add(new FilterImpl(key));
    }

    // ********************内部类********************

    private static class FilterImpl implements LogFilter {
        private final String key;

        public FilterImpl(String key) {
            this.key = key;
        }

        @Override
        public boolean filterLogPack(LogPack logPack) {
            for (LogLine logLine : logPack.logLines) {
                if (logLine.content.contains(key)) {
                    return false;
                }
            }
            // 否则进行过滤
            return true;
        }
    }
}
