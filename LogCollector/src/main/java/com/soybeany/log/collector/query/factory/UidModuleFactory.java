package com.soybeany.log.collector.query.factory;

import com.soybeany.log.collector.common.RangeService;
import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.collector.query.data.QueryContext;
import com.soybeany.log.collector.query.processor.Preprocessor;
import com.soybeany.log.collector.query.processor.RangeLimiter;
import com.soybeany.log.core.model.FileRange;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Soybeany
 * @date 2021/2/1
 */
public class UidModuleFactory implements ModuleFactory {

    private final RangeService rangeService;

    public UidModuleFactory(RangeService rangeService) {
        this.rangeService = rangeService;
    }

    @Override
    public void onSetupPreprocessors(QueryContext context, List<Preprocessor> preprocessors) {
        Set<String> uidSet = context.queryParam.getUidSet();
        if (!uidSet.isEmpty()) {
            preprocessors.add(new LimiterImpl(rangeService, uidSet));
        }
    }

    // ********************内部类********************

    private static class LimiterImpl implements RangeLimiter {
        private final RangeService rangeService;
        private final Set<String> uidSet;

        public LimiterImpl(RangeService rangeService, Set<String> uidSet) {
            this.rangeService = rangeService;
            this.uidSet = uidSet;
        }

        @Override
        public List<FileRange> onSetupQueryRanges(FileRange timeRange, LogIndexes indexes) {
            // 不再需要额外查询
            return Collections.emptyList();
        }

        @Override
        public Set<String> onSetupUnfilteredUidSet(FileRange timeRange, LogIndexes indexes) {
            Set<String> result = new LinkedHashSet<>();
            for (String uid : uidSet) {
                if (indexes.uidRanges.containsKey(uid)) {
                    result.add(uid);
                }
            }
            RangeLimiter.filterUidSetByTimeRange(rangeService, result, timeRange, indexes);
            return result;
        }
    }
}
