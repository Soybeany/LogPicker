package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.core.model.LogResult;
import com.soybeany.log.core.model.LogSection;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/6
 */
public interface LogResultService {

    @NonNull
    List<LogResult> toResults(QueryContext context, List<LogLineInfo> list);

}

@Service
class LogResultServiceImpl implements LogResultService {

    @Override
    public List<LogResult> toResults(QueryContext context, List<LogLineInfo> list) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        // todo 完成实际逻辑
        LogResult result = new LogResult();
        result.uid = "";
        result.sections = new LinkedList<>();
        LogSection section = new LogSection();
        section.logs = new LinkedList<>();
        result.sections.add(section);
        for (LogLineInfo info : list) {
            section.logs.add(info.lineFromByte + "~" + info.toByte);
        }
        return Collections.singletonList(result);
    }
}
