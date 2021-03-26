package com.soybeany.log.collector.query.factory;

import com.soybeany.log.collector.common.service.LogIndexService;
import com.soybeany.log.collector.query.data.QueryContext;
import com.soybeany.log.collector.query.processor.LogFilter;
import com.soybeany.log.collector.query.processor.Preprocessor;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.util.AllKeyContainChecker;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class KeyContainsModuleFactory implements ModuleFactory {

    private static final String PREFIX = "filter";
    private static final String P_KEY_CONTAINS_KEY = "containsKey";

    @Override
    public void onSetupPreprocessors(QueryContext context, List<Preprocessor> preprocessors) {
        String[] keys = context.queryParam.getParam(PREFIX, P_KEY_CONTAINS_KEY);
        // 若没有配置，则不作预处理
        if (null == keys) {
            return;
        }
        // 设置新的过滤器
        keys = LogIndexService.valueToLowerCase(keys);
        preprocessors.add(new FilterImpl(keys));
    }

    // ********************内部类********************

    private static class FilterImpl implements LogFilter {
        private final AllKeyContainChecker checker;

        public FilterImpl(String[] keys) {
            this.checker = new AllKeyContainChecker(keys);
        }

        @Override
        public boolean filterLogPack(LogPack logPack) {
            checker.init();
            for (LogLine logLine : logPack.logLines) {
                if (checker.match(LogIndexService.valueToLowerCase(logLine.content))) {
                    return false;
                }
            }
            // 否则进行过滤
            return true;
        }
    }
}
