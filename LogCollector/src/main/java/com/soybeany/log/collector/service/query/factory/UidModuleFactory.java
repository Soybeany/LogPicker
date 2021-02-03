package com.soybeany.log.collector.service.query.factory;

import com.soybeany.log.collector.service.common.RangeService;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.collector.service.query.processor.Preprocessor;
import com.soybeany.log.collector.service.query.processor.RangeLimiter;
import com.soybeany.log.core.model.FileRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Soybeany
 * @date 2021/2/1
 */
@Component
public class UidModuleFactory implements ModuleFactory {

    @Autowired
    private RangeService rangeService;

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
