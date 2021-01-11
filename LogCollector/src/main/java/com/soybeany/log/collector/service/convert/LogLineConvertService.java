package com.soybeany.log.collector.service.convert;

import com.soybeany.log.collector.repository.FileInfo;
import com.soybeany.log.collector.repository.FileInfoRepository;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.util.file.BdFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author Soybeany
 * @date 2021/1/8
 */
public interface LogLineConvertService {

    LogLine fromInfo(LogLineInfo info);

    LogLineInfo toInfo(int fileId, long fromByte, long toByte, LogLine line);

}

@Service
class LogLineConvertServiceImpl implements LogLineConvertService {

    @Autowired
    private FileInfoRepository fileInfoRepository;

    @Override
    public LogLine fromInfo(LogLineInfo info) {
        LogLine line = new LogLine();
        line.time = info.time;
        line.uid = info.uid;
        line.thread = info.thread;
        line.level = info.level;
        line.content = getContent(info);
        return line;
    }

    @Override
    public LogLineInfo toInfo(int fileId, long fromByte, long toByte, LogLine line) {
        LogLineInfo info = new LogLineInfo();
        info.fileId = fileId;
        info.time = line.time;
        info.uid = line.uid;
        info.thread = line.thread;
        info.level = line.level;
        info.fromByte = fromByte;
        info.toByte = toByte;
        return info;
    }

    private String getContent(LogLineInfo info) {
        File file = getFile(info);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            BdFileUtils.randomRead(file, os, info.fromByte, info.toByte);
            return os.toString("GBK");
        } catch (IOException e) {
            return "日志读取异常";
        }
    }

    private File getFile(LogLineInfo info) {
        FileInfo fileInfo = fileInfoRepository
                .findById(info.fileId)
                .orElseThrow(() -> new LogException("找不到id为“" + info.fileId + "”的文件信息"));
        File file = new File(fileInfo.dir, fileInfo.fileName);
        if (!file.exists()) {
            throw new LogException("找不到“" + fileInfo.fileName + "”的日志文件");
        }
        return file;
    }

}