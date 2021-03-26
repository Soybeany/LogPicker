package com.soybeany.log.collector.query.factory;

import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.service.LogIndexService;
import com.soybeany.log.collector.common.service.RangeService;
import com.soybeany.log.collector.query.data.QueryContext;
import com.soybeany.log.collector.query.data.QueryIndexes;
import com.soybeany.log.collector.query.processor.LogFilter;
import com.soybeany.log.collector.query.processor.Preprocessor;
import com.soybeany.log.collector.query.processor.RangeLimiter;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.util.AllKeyContainChecker;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/23
 */
public class TagContainsModuleFactory implements ModuleFactory {

    private static final String PREFIX = "tag";

    private final LogCollectConfig logCollectConfig;
    private final RangeService rangeService;
    private final LogIndexService logIndexService;

    public TagContainsModuleFactory(LogCollectConfig logCollectConfig) {
        this.logCollectConfig = logCollectConfig;
        this.rangeService = new RangeService(logCollectConfig);
        this.logIndexService = new LogIndexService(logCollectConfig, rangeService);
    }

    @Override
    public void onSetupPreprocessors(QueryContext context, List<Preprocessor> preprocessors) {
        Map<String, String[]> tags = context.queryParam.getParams(PREFIX);
        // 若没有指定tag，则不需要tag预处理
        if (tags.isEmpty()) {
            return;
        }
        // tag预处理并分类
        tags = logIndexService.getTreatedTagMap(tags);
        Map<String, String[]> indexedTagsReceiver = new LinkedHashMap<>();
        Map<String, AllKeyContainChecker> ordinaryTagsReceiver = new LinkedHashMap<>();
        sortTags(tags, indexedTagsReceiver, ordinaryTagsReceiver);
        // 按需创建处理器
        if (!indexedTagsReceiver.isEmpty()) {
            preprocessors.add(new LimiterImpl(rangeService, indexedTagsReceiver));
            context.msgList.add("使用索引型的tags:" + indexedTagsReceiver.keySet());
        }
        if (!ordinaryTagsReceiver.isEmpty()) {
            preprocessors.add(new FilterImpl(logIndexService, ordinaryTagsReceiver));
            context.msgList.add("使用过滤型的tags:" + ordinaryTagsReceiver.keySet());
        }
    }

    // ********************内部方法********************

    private void sortTags(Map<String, String[]> unsortedTags, Map<String, String[]> indexedTagsReceiver, Map<String, AllKeyContainChecker> ordinaryTagsReceiver) {
        unsortedTags.forEach((k, v) -> {
            if (logCollectConfig.tagsToIndex.contains(k)) {
                Optional.ofNullable(indexedTagsReceiver).ifPresent(m -> m.put(k, v));
            } else {
                Optional.ofNullable(ordinaryTagsReceiver).ifPresent(m -> m.put(k, new AllKeyContainChecker(v)));
            }
        });
    }

    // ********************内部类********************

    private static class LimiterImpl implements RangeLimiter {
        private final RangeService rangeService;
        private final Map<String, String[]> tags;

        public LimiterImpl(RangeService rangeService, Map<String, String[]> tags) {
            this.rangeService = rangeService;
            this.tags = tags;
        }

        @Override
        public List<FileRange> onSetupQueryRanges(FileRange timeRange, QueryIndexes indexes) {
            // 不再需要额外查询
            return Collections.emptyList();
        }

        @Override
        public Set<String> onSetupUnfilteredUidSet(FileRange timeRange, QueryIndexes indexes) {
            Set<String> uidSet = null, tempUidSet;
            // 筛选出符合值限制的uid
            for (Map.Entry<String, String[]> entry : tags.entrySet()) {
                for (String tagValue : entry.getValue()) {
                    // 得到uid交集
                    tempUidSet = getUidSet(indexes, entry.getKey(), tagValue);
                    if (null == uidSet) {
                        uidSet = tempUidSet;
                    } else {
                        uidSet.retainAll(tempUidSet);
                    }
                    // 若已无交集，提前返回
                    if (uidSet.isEmpty()) {
                        return uidSet;
                    }
                }
            }
            // 筛选出时间范围内的记录
            RangeLimiter.filterUidSetByTimeRange(rangeService, uidSet, timeRange, indexes);
            return uidSet;
        }

        // ********************内部方法********************

        private Set<String> getUidSet(QueryIndexes indexes, String tagKey, String tagValue) {
            Set<String> result = new LinkedHashSet<>();
            indexes.forEach(tagKey, (value, uidSet) -> {
                if (value.contains(tagValue)) {
                    result.addAll(uidSet);
                }
            });
            return result;
        }
    }

    private static class FilterImpl implements LogFilter {

        private final LogIndexService logIndexService;
        private final Map<String, AllKeyContainChecker> checkers;

        public FilterImpl(LogIndexService logIndexService, Map<String, AllKeyContainChecker> checkers) {
            this.logIndexService = logIndexService;
            this.checkers = checkers;
        }

        @Override
        public boolean filterLogPack(LogPack logPack) {
            List<Map.Entry<String, String>> tagList = logIndexService.getTreatedTagList(logPack.tags);
            for (Map.Entry<String, AllKeyContainChecker> entry : checkers.entrySet()) {
                if (!containValue(tagList, entry.getKey(), entry.getValue())) {
                    return true;
                }
            }
            return false;
        }

        private boolean containValue(List<Map.Entry<String, String>> tagList, String tagKey, AllKeyContainChecker checker) {
            checker.init();
            for (Map.Entry<String, String> entry : tagList) {
                if (tagKey.equals(entry.getKey()) && checker.match(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }
    }

}
