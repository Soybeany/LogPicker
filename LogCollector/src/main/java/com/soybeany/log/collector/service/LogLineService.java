package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.LogLine;
import com.soybeany.log.collector.repository.FileInfoRepository;
import com.soybeany.log.collector.repository.LogLineInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public interface LogLineService {

    LogLine parseToLogLine(LogLineInfo info);

}

@Service
class LogLineServiceImpl implements LogLineService {

    @Autowired
    private FileInfoRepository fileInfoRepository;

    @Override
    public LogLine parseToLogLine(LogLineInfo info) {
        return null;
    }
}