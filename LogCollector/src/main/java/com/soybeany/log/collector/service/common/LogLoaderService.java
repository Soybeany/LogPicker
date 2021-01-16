package com.soybeany.log.collector.service.common;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.model.FileRange;
import com.soybeany.log.collector.service.common.model.ILogReceiver;
import com.soybeany.log.collector.service.common.parser.LogParser;
import com.soybeany.log.core.model.LogException;
import com.soybeany.util.file.BdFileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Soybeany
 * @date 2021/1/14
 */
public interface LogLoaderService {

    void load(File file, List<FileRange> ranges, ILogReceiver receiver) throws IOException;

}

@Service
class LogLoaderServiceImpl implements LogLoaderService {

    private static final int STATE_CONTINUE = 0;
    private static final int STATE_ABORT = 1;

    private final AppConfig appConfig;
    private final LogParser logParser;

    public LogLoaderServiceImpl(AppConfig appConfig, Map<String, LogParser> parserMap) {
        this.appConfig = appConfig;
        this.logParser = Optional
                .ofNullable(parserMap.get(appConfig.logParseMode + "LogParser"))
                .orElseThrow(() -> new LogException("没有找到指定的日志解析模式"));
    }

    @Override
    public void load(File file, List<FileRange> ranges, ILogReceiver receiver) throws IOException {
        if (!file.exists()) {
            throw new LogException("找不到名称为“" + file.getName() + "”的日志文件");
        }
        receiver.onStart();
        logParser.beforeBatchParse();
        long bytesRead = 0, endPointer = 0;
        for (FileRange range : ranges) {
            ReadLineCallbackImpl callback = new ReadLineCallbackImpl(range.to, receiver);
            BdFileUtils.randomReadLine(file, range.from, callback);
            bytesRead += callback.bytesRead;
            endPointer = range.from + callback.bytesRead;
        }
        logParser.afterBatchParse(receiver);
        receiver.onFinish(bytesRead, endPointer);
    }

    // ********************内部类********************

    private class ReadLineCallbackImpl implements BdFileUtils.RandomReadLineCallback {
        private final long targetEnd;
        private final ILogReceiver receiver;

        long bytesRead;

        public ReadLineCallbackImpl(long targetEnd, ILogReceiver receiver) {
            this.targetEnd = targetEnd;
            this.receiver = receiver;
        }

        @Override
        public String onSetupCharset() {
            return appConfig.logCharset;
        }

        @Override
        public int onHandleLine(long startPointer, long endPointer, String line) {
            // 若已读到限制的字节，则不再继续
            if (endPointer > targetEnd) {
                return STATE_ABORT;
            }
            bytesRead += (endPointer - startPointer);
            logParser.onParse(appConfig.lineParsePattern, startPointer, endPointer, line, receiver);
            return STATE_CONTINUE;
        }
    }
}
