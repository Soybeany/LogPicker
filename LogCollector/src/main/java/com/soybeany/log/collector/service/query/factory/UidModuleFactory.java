package com.soybeany.log.collector.service.query.factory;

import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.collector.service.query.processor.Preprocessor;
import com.soybeany.log.collector.service.query.processor.RangeLimiter;
import com.soybeany.log.core.model.FileRange;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Soybeany
 * @date 2021/2/1
 */
@Component
public class UidModuleFactory implements ModuleFactory {

    @Override
    public void onSetupPreprocessors(QueryContext context, List<Preprocessor> preprocessors) {
        Set<String> uidSet = context.queryParam.getUidSet();
        if (!uidSet.isEmpty()) {
            preprocessors.add(new LimiterImpl(uidSet));
        }
    }

    // ********************内部类********************

    private static class LimiterImpl implements RangeLimiter {
        private final Set<String> uidSet;

        public LimiterImpl(Set<String> uidSet) {
            this.uidSet = uidSet;
        }

        @Override
        public Set<String> onSetupUnfilteredUidSet(FileRange timeRange, LogIndexes indexes) {
            Set<String> result = new LinkedHashSet<>();
            for (String uid : uidSet) {
                if (indexes.uidRanges.containsKey(uid)) {
                    result.add(uid);
                }
            }
            return result;
        }
    }
}
