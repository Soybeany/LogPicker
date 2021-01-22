package com.soybeany.log.collector.service.query;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.BytesRangeService;
import com.soybeany.log.collector.service.common.LogIndexService;
import com.soybeany.log.collector.service.common.model.FileRange;
import com.soybeany.log.collector.service.common.model.LogIndexes;
import com.soybeany.log.collector.service.query.model.ITagChecker;
import com.soybeany.log.collector.service.query.model.QueryParam;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.model.LogTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
public interface TagInfoService extends ITagChecker {

    /**
     * @return 是否包含标签
     */
    boolean initTags(QueryParam param);

    @NonNull
    List<FileRange> getIntersectedRanges(LogIndexes indexes, QueryParam param);

}

@Service
class TagInfoServiceImpl implements TagInfoService {

    private static final String PREFIX = "tag";

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private LogIndexService logIndexService;
    @Autowired
    private BytesRangeService bytesRangeService;

    @Override
    public boolean initTags(QueryParam param) {
        Map<String, String> map = param.getParams(PREFIX);
        map.forEach((key, value) -> map.put(key, value.toLowerCase()));
        return !map.isEmpty();
    }

    @Override
    public List<FileRange> getIntersectedRanges(LogIndexes indexes, QueryParam param) {
        List<List<FileRange>> rangeList = new LinkedList<>();
        // 时间范围
        TreeMap<String, Long> timeIndexMap = indexes.timeIndexMap;
        long startByte = Optional.ofNullable(timeIndexMap.floorEntry(param.getFromTime()))
                .map(Map.Entry::getValue).orElse(0L);
        long endByte = Optional.ofNullable(timeIndexMap.ceilingEntry(param.getToTime()))
                .map(Map.Entry::getValue).orElse(indexes.scannedBytes);
        rangeList.add(Collections.singletonList(new FileRange(startByte, endByte)));
        // tag范围
        param.getParams(PREFIX).forEach((key, value) -> {
            if (!appConfig.tagsToIndex.contains(key)) {
                throw new LogException("使用了未索引的标签:" + key);
            }
            List<FileRange> ranges = logIndexService.getRangesOfTag(indexes, key, value);
            rangeList.add(ranges);
        });
        return bytesRangeService.intersect(rangeList);
    }

    @Override
    public boolean containsAllTags(QueryParam param, LogPack logPack) {
        for (Map.Entry<String, String> entry : param.getParams(PREFIX).entrySet()) {
            boolean isMatch = false;
            for (LogTag tag : logPack.tags) {
                if (tag.key.equals(entry.getKey()) && tag.value.contains(entry.getValue())) {
                    isMatch = true;
                    break;
                }
            }
            if (!isMatch) {
                return false;
            }
        }
        return true;
    }
}
