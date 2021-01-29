package com.soybeany.log.collector.service.query.factory;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.BytesRangeService;
import com.soybeany.log.collector.service.common.LogIndexService;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.collector.service.query.model.LogFilter;
import com.soybeany.log.collector.service.query.model.RangeLimiter;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogException;
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
    public RangeLimiter getNewRangeLimiterIfInNeed(QueryContext context) {
        Map<String, String> tags = context.queryParam.getParams(PREFIX);
        // 若没有指定tag，则不需要tag范围过滤
        if (tags.isEmpty()) {
            return null;
        }
        tags = logIndexService.getTreatedTagMap(tags);
        return new LimiterImpl(appConfig, bytesRangeService, tags);
    }

    @Override
    public LogFilter getNewLogFilterIfInNeed(QueryContext context) {
        return null;
    }

    // ********************内部类********************

    private static class LimiterImpl implements RangeLimiter {
        private final AppConfig appConfig;
        private final BytesRangeService bytesRangeService;
        private final Map<String, String> tags;

        public LimiterImpl(AppConfig appConfig, BytesRangeService bytesRangeService, Map<String, String> tags) {
            this.appConfig = appConfig;
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
                // 只允许查询中使用支持索引的标签
                if (!appConfig.tagsToIndex.contains(entry.getKey())) {
                    throw new LogException("使用了未索引的标签:" + entry.getKey());
                }
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
}
