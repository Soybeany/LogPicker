package com.soybeany.log.collector.service.query.filter;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.BytesRangeService;
import com.soybeany.log.collector.service.common.LogIndexService;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.common.model.LogFilter;
import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.model.LogTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/23
 */
@Component
class TagContainsLogFilterFactory implements LogFilterFactory {

    private static final String PREFIX = "tag";

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private LogIndexService logIndexService;
    @Autowired
    private BytesRangeService bytesRangeService;

    @Override
    public LogFilter getNewLogFilterIfInNeed(QueryContext context) {
        Map<String, String> tags = context.queryParam.getParams(PREFIX);
        // 若没有指定tag，则不需要tag过滤器
        if (tags.isEmpty()) {
            return null;
        }
        tags = logIndexService.getTreatedTagMap(tags);
        return new FilterImpl(appConfig, bytesRangeService, tags);
    }

    // ********************内部类********************

    private static class FilterImpl implements LogFilter {

        private final AppConfig appConfig;
        private final BytesRangeService bytesRangeService;
        private final Map<String, String> tags;

        public FilterImpl(AppConfig appConfig, BytesRangeService bytesRangeService, Map<String, String> tags) {
            this.appConfig = appConfig;
            this.bytesRangeService = bytesRangeService;
            this.tags = tags;
        }

        @Override
        public void onSetupRanges(Map<String, Map<File, List<FileRange>>> uidMap, FileRange timeRange, LogIndexes indexes) {
            tags.forEach((tagKey, tagValue) -> {
                if (!appConfig.tagsToIndex.contains(tagKey)) {
                    throw new LogException("使用了未索引的标签:" + tagKey);
                }
                setupRangesOfTag(uidMap, timeRange, indexes, tagKey, tagValue);
            });
        }

        @Override
        public boolean filterLogPack(LogPack logPack) {
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                boolean isMatch = false;
                for (LogTag tag : logPack.tags) {
                    if (tag.key.equals(entry.getKey()) && tag.value.contains(entry.getValue())) {
                        isMatch = true;
                        break;
                    }
                }
                if (!isMatch) {
                    return true;
                }
            }
            return false;
        }

        // ********************内部方法********************

        private void setupRangesOfTag(Map<String, Map<File, List<FileRange>>> uidMap, FileRange timeRange, LogIndexes indexes, String tagKey, String tagValue) {
            Optional.ofNullable(indexes.tagUidMap.get(tagKey)).ifPresent(map -> map.forEach((value, uidSet) -> {
                if (!value.contains(tagValue)) {
                    return;
                }
                List<FileRange> timeRanges = Collections.singletonList(timeRange);
                for (String uid : uidSet) {
                    LinkedList<FileRange> uidRanges = indexes.uidRanges.get(uid);
                    List<FileRange> intersect = bytesRangeService.intersect(Arrays.asList(uidRanges, timeRanges));
                    // 若时间有交集，才添加到候选列表中
                    if (!intersect.isEmpty()) {
                        uidMap.computeIfAbsent(uid, k -> new LinkedHashMap<>()).put(indexes.logFile, uidRanges);
                    }
                }
            }));
        }
    }
}
