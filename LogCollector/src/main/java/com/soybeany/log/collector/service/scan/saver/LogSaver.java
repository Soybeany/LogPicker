package com.soybeany.log.collector.service.scan.saver;

import com.soybeany.log.collector.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public interface LogSaver {

    void save(FileInfo info, List<LogTagInfo> tagList, List<LogLineInfo> lineList);

}

@Component
@Transactional
class LogSaverImpl implements LogSaver {

    @Autowired
    private FileInfoRepository fileInfoRepository;
    @Autowired
    private LogTagInfoRepository logTagInfoRepository;
    @Autowired
    private LogLineInfoRepository logLineInfoRepository;

    @Override
    public void save(FileInfo info, List<LogTagInfo> tagList, List<LogLineInfo> lineList) {
        fileInfoRepository.save(info);
        logTagInfoRepository.saveAll(tagList);
        logLineInfoRepository.saveAll(lineList);
    }
}
