package com.soybeany.log.collector.service.query.filter;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.BytesRangeService;
import com.soybeany.log.collector.service.common.model.FileRange;
import com.soybeany.log.collector.service.common.model.LogIndexes;
import com.soybeany.log.collector.service.query.model.ILogFilter;
import com.soybeany.log.collector.service.query.model.QueryContext;
import com.soybeany.log.collector.service.query.model.QueryParam;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.model.LogTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/23
 */
@Component
class TagContainsLogFilter implements LogFilterFactory {

    private static final String PREFIX = "tag";

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private BytesRangeService bytesRangeService;

    @Override
    public ILogFilter getNewLogFilterIfInNeed(QueryContext context) {
        Map<String, String> tags = context.queryParam.getParams(PREFIX);
        // 若没有指定tag，则不需要tag过滤器
        if (tags.isEmpty()) {
            return null;
        }
        return new FilterImpl(appConfig, bytesRangeService, tags, context.queryParam);
    }

    // ********************内部类********************

    private static class FilterImpl implements ILogFilter {

        private final AppConfig appConfig;
        private final BytesRangeService bytesRangeService;
        private final Map<String, String> tags = new HashMap<>();
        private final String fromTime;
        private final String toTime;

        public FilterImpl(AppConfig appConfig, BytesRangeService bytesRangeService, Map<String, String> tags, QueryParam param) {
            this.appConfig = appConfig;
            this.bytesRangeService = bytesRangeService;
            tags.forEach((k, v) -> {
                this.tags.put(k, v.toLowerCase());
            });
            this.fromTime = param.getFromTime();
            this.toTime = param.getToTime();
        }

        @Override
        public List<FileRange> getFilteredRanges(LogIndexes indexes) {
            List<List<FileRange>> rangeList = new LinkedList<>();
            // 时间范围
            TreeMap<String, Long> timeIndexMap = indexes.timeIndexMap;
            long startByte = Optional.ofNullable(timeIndexMap.floorEntry(fromTime))
                    .map(Map.Entry::getValue).orElse(0L);
            long endByte = Optional.ofNullable(timeIndexMap.ceilingEntry(toTime))
                    .map(Map.Entry::getValue).orElse(indexes.scannedBytes);
            rangeList.add(Collections.singletonList(new FileRange(startByte, endByte)));
            // tag范围
            tags.forEach((tagKey, tagValue) -> {
                if (!appConfig.tagsToIndex.contains(tagKey)) {
                    throw new LogException("使用了未索引的标签:" + tagKey);
                }
                List<FileRange> ranges = getRangesOfTag(indexes, tagKey, tagValue);
                rangeList.add(ranges);
            });
            return bytesRangeService.intersect(rangeList);
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

        private List<FileRange> getRangesOfTag(LogIndexes indexes, String tagKey, String tagValue) {
            List<FileRange> ranges = new LinkedList<>();
            indexes.tagsIndexMap.get(tagKey).forEach((value, range) -> {
                if (value.contains(tagValue)) {
                    ranges.addAll(range);
                }
            });
            return ranges;
        }
    }
}
