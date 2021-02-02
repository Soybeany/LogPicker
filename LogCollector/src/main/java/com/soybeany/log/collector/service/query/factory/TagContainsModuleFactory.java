package com.soybeany.log.collector.service.query.factory;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.BytesRangeService;
import com.soybeany.log.collector.service.common.LogIndexService;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.collector.service.query.processor.LogFilter;
import com.soybeany.log.collector.service.query.processor.Preprocessor;
import com.soybeany.log.collector.service.query.processor.RangeLimiter;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.model.LogTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/23
 */
@Component
class TagContainsModuleFactory implements ModuleFactory {

    private static final String PREFIX = "tag";

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private LogIndexService logIndexService;
    @Autowired
    private BytesRangeService bytesRangeService;

    @Override
    public void onSetupPreprocessors(QueryContext context, List<Preprocessor> preprocessors) {
        Map<String, String> tags = context.queryParam.getParams(PREFIX);
        // 若没有指定tag，则不需要tag预处理
        if (tags.isEmpty()) {
            return;
        }
        // tag预处理并分类
        tags = logIndexService.getTreatedTagMap(tags);
        Map<String, String> indexedTagsReceiver = new HashMap<>();
        Map<String, String> ordinaryTagsReceiver = new HashMap<>();
        sortTags(tags, indexedTagsReceiver, ordinaryTagsReceiver);
        // 按需创建处理器
        if (!indexedTagsReceiver.isEmpty()) {
            preprocessors.add(new LimiterImpl(bytesRangeService, indexedTagsReceiver));
            context.msgList.add("使用索引的tags:" + indexedTagsReceiver.keySet());
        }
        if (!ordinaryTagsReceiver.isEmpty()) {
            preprocessors.add(new FilterImpl(ordinaryTagsReceiver));
            context.msgList.add("使用过滤的tags:" + ordinaryTagsReceiver.keySet());
        }
    }

    // ********************内部方法********************

    private void sortTags(Map<String, String> unsortedTags, @Nullable Map<String, String> indexedTagsReceiver, @Nullable Map<String, String> ordinaryTagsReceiver) {
        unsortedTags.forEach((k, v) -> {
            if (appConfig.tagsToIndex.contains(k)) {
                Optional.ofNullable(indexedTagsReceiver).ifPresent(m -> m.put(k, v));
            } else {
                Optional.ofNullable(ordinaryTagsReceiver).ifPresent(m -> m.put(k, v));
            }
        });
    }

    // ********************内部类********************

    private static class LimiterImpl implements RangeLimiter {
        private final BytesRangeService bytesRangeService;
        private final Map<String, String> tags;

        public LimiterImpl(BytesRangeService bytesRangeService, Map<String, String> tags) {
            this.bytesRangeService = bytesRangeService;
            this.tags = tags;
        }

        @Override
        public List<FileRange> onSetupQueryRanges(FileRange timeRange, LogIndexes indexes) {
            // 不再需要额外查询
            return Collections.emptyList();
        }

        @Override
        public Set<String> onSetupUnfilteredUidSet(FileRange timeRange, LogIndexes indexes) {
            Set<String> uidSet = null, tempUidSet;
            // 筛选出符合值限制的uid
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                // 得到uid交集
                tempUidSet = getUidSet(indexes, entry.getKey(), entry.getValue());
                if (null == uidSet) {
                    uidSet = tempUidSet;
                } else {
                    uidSet.retainAll(tempUidSet);
                }
                // 若已无交集，提前返回
                if (uidSet.isEmpty()) {
                    return null;
                }
            }
            // 筛选出时间范围内的记录
            filterUidSetByTimeRange(uidSet, timeRange, indexes);
            return uidSet;
        }

        // ********************内部方法********************

        private void filterUidSetByTimeRange(@Nullable Set<String> uidSet, FileRange timeRange, LogIndexes indexes) {
            if (null == uidSet) {
                return;
            }
            List<FileRange> timeRanges = Collections.singletonList(timeRange);
            Iterator<String> uidIterator = uidSet.iterator();
            while (uidIterator.hasNext()) {
                String uid = uidIterator.next();
                LinkedList<FileRange> uidRanges = indexes.uidRanges.get(uid);
                List<FileRange> intersect = bytesRangeService.intersect(Arrays.asList(uidRanges, timeRanges));
                // 若时间无交集，则移除
                if (intersect.isEmpty()) {
                    uidIterator.remove();
                }
            }
        }

        @NonNull
        private Set<String> getUidSet(LogIndexes indexes, String tagKey, String tagValue) {
            Map<String, Set<String>> tagValueMap = indexes.tagUidMap.get(tagKey);
            if (null == tagValueMap) {
                return Collections.emptySet();
            }
            Set<String> result = new LinkedHashSet<>();
            tagValueMap.forEach((value, uidSet) -> {
                if (value.contains(tagValue)) {
                    result.addAll(uidSet);
                }
            });
            return result;
        }
    }

    private static class FilterImpl implements LogFilter {

        private final Map<String, String> tags;

        public FilterImpl(Map<String, String> tags) {
            this.tags = tags;
        }

        @Override
        public boolean filterLogPack(LogPack logPack) {
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                String value = getValue(logPack, entry.getKey());
                if (null == value || !value.contains(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }

        private String getValue(LogPack logPack, String tagKey) {
            for (LogTag tag : logPack.tags) {
                if (tagKey.equals(tag.key)) {
                    return tag.value;
                }
            }
            return null;
        }
    }

}
