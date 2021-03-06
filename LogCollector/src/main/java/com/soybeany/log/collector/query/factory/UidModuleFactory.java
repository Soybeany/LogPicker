package com.soybeany.log.collector.query.factory;

import com.soybeany.log.collector.common.service.RangeService;
import com.soybeany.log.collector.query.data.QueryContext;
import com.soybeany.log.collector.query.data.QueryIndexes;
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
        public List<FileRange> onSetupQueryRanges(FileRange timeRange, QueryIndexes indexes) {
            // 不再需要额外查询
            return Collections.emptyList();
        }

        @Override
        public Set<String> onSetupUnfilteredUidSet(FileRange timeRange, QueryIndexes indexes) {
            Set<String> result = new LinkedHashSet<>();
            for (String uid : uidSet) {
                if (indexes.containUid(uid)) {
                    result.add(uid);
                }
            }
            RangeLimiter.filterUidSetByTimeRange(rangeService, result, timeRange, indexes);
            return result;
        }
    }
}
