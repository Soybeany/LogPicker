package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.LogLine;
import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.RawLogResult;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.TagInfo;
import com.soybeany.log.core.model.LogResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public interface ModelConvertService {

    RawLogResult toRawResult(String uid, String thread, @Nullable List<TagInfo> tags, @Nullable List<LogLine> lines);

    List<LogLine> toLineList(List<LogLineInfo> list);

    @NonNull
    List<LogResult> toLogResults(QueryContext context, List<RawLogResult> list);

}

@Service
class ModelConvertServiceImpl implements ModelConvertService {

    @Autowired
    private LogLineService logLineService;

    @Override
    public List<LogLine> toLineList(List<LogLineInfo> list) {
        List<LogLine> result = new LinkedList<>();
        for (LogLineInfo info : list) {
            result.add(logLineService.parseToLogLine(info));
        }
        return result;
    }

    @Override
    public RawLogResult toRawResult(String uid, String thread, @Nullable List<TagInfo> tags, @Nullable List<LogLine> lines) {
        RawLogResult result = new RawLogResult();
        result.uid = uid;
        result.thread = thread;
        if (null != tags) {
            result.tags = new HashMap<>();
            for (TagInfo tag : tags) {
                result.tags.put(tag.key, tag.value);
            }
        }
        if (null != lines) {
            result.logLines.addAll(lines);
        }
        return result;
    }

    @Override
    public List<LogResult> toLogResults(QueryContext context, List<RawLogResult> list) {
        // todo 拼装时间时，在日期右侧显示+n代表跨天，如:00:02(+1)
        for (RawLogResult result : list) {

        }
        return Collections.emptyList();
    }

}