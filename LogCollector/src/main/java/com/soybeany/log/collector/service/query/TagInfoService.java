package com.soybeany.log.collector.service.query;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.BytesRangeService;
import com.soybeany.log.collector.service.common.LogIndexService;
import com.soybeany.log.collector.service.common.model.FileRange;
import com.soybeany.log.collector.service.common.model.LogIndexes;
import com.soybeany.log.collector.service.query.model.QueryParam;
import com.soybeany.log.core.model.LogException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/4
 */
public interface TagInfoService {

    boolean hasTags(QueryParam param);

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
    public boolean hasTags(QueryParam param) {
        return !param.getParams(PREFIX).isEmpty();
    }

    @Override
    public List<FileRange> getIntersectedRanges(LogIndexes indexes, QueryParam param) {
        List<List<FileRange>> rangeList = new LinkedList<>();
        param.getParams(PREFIX).forEach((key, value) -> {
            if (!appConfig.tagsToIndex.contains(key)) {
                throw new LogException("使用了未索引的标签:" + key);
            }
            List<FileRange> ranges = logIndexService.getRangesOfTag(indexes, key, value);
            rangeList.add(ranges);
        });
        return bytesRangeService.intersect(rangeList);
    }
}
