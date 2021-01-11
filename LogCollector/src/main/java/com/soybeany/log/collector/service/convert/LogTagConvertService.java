package com.soybeany.log.collector.service.convert;

import com.soybeany.log.collector.repository.LogTagInfo;
import com.soybeany.log.core.model.LogTag;
import org.springframework.stereotype.Service;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public interface LogTagConvertService {

    LogTag fromInfo(LogTagInfo info);

    LogTagInfo toInfo(int fileId, LogTag tag);

}

@Service
class LogTagConvertServiceImpl implements LogTagConvertService {

    @Override
    public LogTag fromInfo(LogTagInfo info) {
        LogTag tag = new LogTag();
        tag.uid = info.uid;
        tag.thread = info.thread;
        tag.time = info.time;
        tag.key = info.key;
        tag.value = info.value;
        return tag;
    }

    @Override
    public LogTagInfo toInfo(int fileId, LogTag tag) {
        LogTagInfo info = new LogTagInfo();
        info.fileId = fileId;
        info.uid = tag.uid;
        info.thread = tag.thread;
        info.time = tag.time;
        info.key = tag.key;
        info.value = tag.value;
        return info;
    }
}
