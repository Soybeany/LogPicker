package com.soybeany.log.collector.service.scan.importer;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.repository.FileInfo;
import com.soybeany.log.collector.repository.FileInfoRepository;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.LogTagInfo;
import com.soybeany.log.collector.service.convert.LogLineConvertService;
import com.soybeany.log.collector.service.convert.LogTagConvertService;
import com.soybeany.log.collector.service.scan.parser.LogParser;
import com.soybeany.log.collector.service.scan.saver.LogSaver;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
@Scope("prototype")
@Component
class StdImporter implements LogImporter {

    @Autowired
    private LogLineConvertService logLineConvertService;
    @Autowired
    private LogTagConvertService logTagConvertService;
    @Autowired
    private FileInfoRepository fileInfoRepository;
    @Autowired
    private LogSaver logSaver;

    private final AppConfig appConfig;
    private final LogParser logParser;
    private Pattern linePattern;

    private final List<LogTagInfo> tagBufferList = new LinkedList<>();
    private final List<LogLineInfo> lineBufferList = new LinkedList<>();

    private FileInfo fileInfo;
    private int readLines;

    private LogLine lastLogLine;
    private StringBuilder tempContent;
    private int lastLogStartIndex;
    private long fromByte;
    private long toByte;

    public StdImporter(AppConfig appConfig, Map<String, LogParser> parsers) {
        this.appConfig = appConfig;
        this.logParser = Optional
                .ofNullable(parsers.get(appConfig.logParseMode + "LogParser"))
                .orElseThrow(() -> new LogException("没有找到指定的日志解析模式"));
    }

    @Override
    public void onStart(int fileId, File file) {
        this.fileInfo = fileInfoRepository.findById(fileId).orElseThrow(() -> new LogException("没有找到指定的文件"));
    }

    @Override
    public void onRead(long startPointer, long endPointer, String line) {
        LogLine curLogLine = logParser.parseToLogLine(linePattern, line);
        // 尝试日志拼接
        if (null == curLogLine) {
            // 舍弃无法拼接的日志
            if (null == lastLogLine) {
                return;
            }
            if (null == tempContent) {
                tempContent = new StringBuilder(lastLogLine.content);
            }
            tempContent.append(line);
            toByte = endPointer;
            return;
        }
        // 更新已扫描的字节
        fileInfo.scannedBytes = endPointer;
        // 处理lastLogLine
        handleLastLogLine();
        // 尝试保存
        tryToSave();
        // 将lastLogLine替换为当前LogLine
        lastLogLine = curLogLine;
        lastLogStartIndex = line.length() - curLogLine.content.length();
        fromByte = startPointer;
        toByte = endPointer;
    }

    @Override
    public void onFinish() {
        handleLastLogLine();
        logSaver.save(fileInfo, tagBufferList, lineBufferList);
    }

    // ********************内部方法********************

    @PostConstruct
    private void onInit() {
        linePattern = Pattern.compile(appConfig.lineParseRegex);
    }

    private void handleLastLogLine() {
        if (null == lastLogLine) {
            return;
        }
        // 若有必要，进行日志替换
        if (null != tempContent) {
            lastLogLine.content = tempContent.toString();
            tempContent = null;
        }
        // 标签处理
        List<LogTag> tags = logParser.parseToLogTags(lastLogLine);
        if (null != tags) {
            for (LogTag tag : tags) {
                LogTagInfo info = logTagConvertService.toInfo(fileInfo.getId(), tag);
                tagBufferList.add(info);
            }
        } else {
            LogLineInfo info = logLineConvertService.toInfo(fileInfo.getId(), fromByte, toByte, lastLogStartIndex, lastLogLine);
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

}
