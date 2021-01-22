package com.soybeany.log.collector.service.common;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.model.FileRange;
import com.soybeany.log.collector.service.common.model.ILogReceiver;
import com.soybeany.log.core.model.LogException;
import com.soybeany.util.file.BdFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/14
 */
public interface LogLoaderService {

    int load(File file, List<FileRange> ranges, ILogReceiver receiver) throws IOException;

}

@Service
class LogLoaderServiceImpl implements LogLoaderService {

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private LogParserService logParserService;

    @Override
    public int load(File file, List<FileRange> ranges, ILogReceiver receiver) throws IOException {
        if (!file.exists()) {
            throw new LogException("找不到名称为“" + file.getName() + "”的日志文件");
        }
        long bytesRead = 0, endPointer = 0;
        try {
            receiver.onStart();
            logParserService.beforeBatchParse(receiver);
            for (FileRange range : ranges) {
                ReadLineCallbackImpl callback = new ReadLineCallbackImpl(range.to, receiver);
                BdFileUtils.randomReadLine(file, range.from, callback);
                bytesRead += callback.bytesRead;
                endPointer = range.from + callback.bytesRead;
                // 按需提前中断
                if (ILogReceiver.STATE_CONTINUE != callback.status) {
                    return callback.status;
                }
            }
        } finally {
            logParserService.afterBatchParse(receiver);
            receiver.onFinish(bytesRead, endPointer);
        }
        return ILogReceiver.STATE_CONTINUE;
    }

    // ********************内部类********************

    private class ReadLineCallbackImpl implements BdFileUtils.RandomReadLineCallback {
        private final long targetEnd;
        private final ILogReceiver receiver;

        long bytesRead;
        int status;

        public ReadLineCallbackImpl(long targetEnd, ILogReceiver receiver) {
            this.targetEnd = targetEnd;
            this.receiver = receiver;
        }

        @Override
        public String onSetupCharset() {
            return appConfig.logCharset;
        }

        @Override
        public void onFinish(int status) {
            this.status = status;
        }

        @Override
        public int onHandleLine(long startPointer, long endPointer, String line) {
            // 若已读到限制的字节，则不再继续
            if (endPointer > targetEnd) {
                return ILogReceiver.STATE_ABORT;
            }
            bytesRead += (endPointer - startPointer);
            logParserService.onParse(appConfig.lineParsePattern, startPointer, endPointer, line, receiver);
            return receiver.onGetState();
        }
    }

}
