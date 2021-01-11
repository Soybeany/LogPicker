package com.soybeany.log.collector.service.scan.importer;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.repository.FileInfo;
import com.soybeany.log.collector.repository.FileInfoRepository;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.LogTagInfo;
import com.soybeany.log.collector.service.convert.LogLineConvertService;
import com.soybeany.log.collector.service.convert.LogTagConvertService;
import com.soybeany.log.collector.service.scan.parser.LineParser;
import com.soybeany.log.collector.service.scan.parser.TagParser;
import com.soybeany.log.collector.service.scan.saver.LogSaver;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
@Scope("prototype")
@Component
class StdImporter implements LogImporter {

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private LogLineConvertService logLineConvertService;
    @Autowired
    private LogTagConvertService logTagConvertService;
    @Autowired
    private FileInfoRepository fileInfoRepository;
    @Autowired
    private LogSaver logSaver;

    private final LineParser lineParser;
    private final TagParser tagParser;

    private FileInfo fileInfo;
    private LogLineContainer lastLogLine;
    private final List<LogTagInfo> tagBufferList = new LinkedList<>();
    private final List<LogLineInfo> lineBufferList = new LinkedList<>();
    private int readLines;

    public StdImporter(LineParser lineParser, TagParser tagParser) {
        this.lineParser = lineParser;
        this.tagParser = tagParser;
    }

    @Override
    public void onStart(int fileId, File file) {
        this.fileInfo = fileInfoRepository.findById(fileId).orElseThrow(() -> new LogException("没有找到指定的文件"));
    }

    @Override
    public void onRead(long startPointer, long endPointer, String line) {
        LogLine curLogLine = lineParser.parse(line);
        // 尝试日志拼接
        if (null == curLogLine) {
            // 舍弃无法拼接的日志
            if (null == lastLogLine) {
                return;
            }
            if (null == lastLogLine.contentBuilder) {
                lastLogLine.contentBuilder = new StringBuilder(lastLogLine.logLine.content);
            }
            lastLogLine.contentBuilder.append(line);
            lastLogLine.toByte = endPointer;
            return;
        }
        // 更新已扫描的字节
        fileInfo.scannedBytes = endPointer;
        // 处理lastLogLine
        handleLastLogLine();
        // 尝试保存
        tryToSave();
        // 将lastLogLine替换为当前LogLine
        lastLogLine = new LogLineContainer(curLogLine, startPointer, endPointer);
    }

    @Override
    public void onFinish() {
        handleLastLogLine();
        logSaver.save(fileInfo, tagBufferList, lineBufferList);
    }

    // ********************内部方法********************

    private void handleLastLogLine() {
        if (null == lastLogLine) {
            return;
        }
        // 若有必要，进行日志替换
        if (null != lastLogLine.contentBuilder) {
            lastLogLine.logLine.content = lastLogLine.contentBuilder.toString();
            lastLogLine.contentBuilder = null;
        }
        // 标签处理
        LogTag logTag = tagParser.parse(lastLogLine.logLine);
        if (null != logTag) {
            LogTagInfo info = logTagConvertService.toInfo(fileInfo.getId(), logTag);
            tagBufferList.add(info);
        } else {
            LogLineInfo info = logLineConvertService.toInfo(fileInfo.getId(), lastLogLine.fromByte, lastLogLine.toByte, lastLogLine.logLine);
            lineBufferList.add(info);
        }
    }

    private void tryToSave() {
        if (++readLines < appConfig.linesToBatchSave) {
            return;
        }
        // 保存
        logSaver.save(fileInfo, tagBufferList, lineBufferList);
        // 重置
        tagBufferList.clear();
        lineBufferList.clear();
        readLines = 0;
    }

    // ********************内部类区********************

    private static class LogLineContainer {
        LogLine logLine;

        StringBuilder contentBuilder;
        long fromByte;
        long toByte;

        public LogLineContainer(LogLine logLine, long fromByte, long toByte) {
            this.logLine = logLine;
            this.fromByte = fromByte;
            this.toByte = toByte;
        }
    }

}
