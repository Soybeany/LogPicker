package com.soybeany.log.collector.service.convert;

import com.soybeany.log.collector.repository.FileInfo;
import com.soybeany.log.collector.repository.FileInfoRepository;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.util.file.BufferedRandomAccessFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;

/**
 * @author Soybeany
 * @date 2021/1/8
 */
public interface LogLineConvertService {

    LogLine fromInfo(LogLineInfo info);

    LogLineInfo toInfo(int fileId, long fromByte, long toByte, int logStartIndex, LogLine line);

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
        line.content = getContent(info).substring(info.logStartIndex);
        return line;
    }

    @Override
    public LogLineInfo toInfo(int fileId, long fromByte, long toByte, int logStartIndex, LogLine line) {
        LogLineInfo info = new LogLineInfo();
        info.fileId = fileId;
        info.time = line.time;
        info.uid = line.uid;
        info.thread = line.thread;
        info.level = line.level;
        info.fromByte = fromByte;
        info.toByte = toByte;
        info.logStartIndex = logStartIndex;
        return info;
    }

    private String getContent(LogLineInfo info) {
        File file = getFile(info);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            randomRead(file, os, info.fromByte, info.toByte);
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

    private long randomRead(File inFile, OutputStream out, long start, long end) throws IOException {
        try (RandomAccessFile raf = new BufferedRandomAccessFile(inFile, "r");) {
            raf.seek(start);
            int bufferSize = 25 * 1024;
            byte[] tempArr = new byte[bufferSize];
            int curRead = 0;
            long totalRead = 0, delta = end - start;
            while (totalRead <= delta - bufferSize) {
                curRead = raf.read(tempArr, 0, bufferSize);
                totalRead += curRead;
                out.write(tempArr, 0, curRead);
            }
            while (totalRead < delta && -1 != curRead) {
                curRead = raf.read(tempArr, 0, (int) (delta - totalRead));
                totalRead += curRead;
                out.write(tempArr, 0, curRead);
            }
            return totalRead;
        }
    }

}