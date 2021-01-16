package com.soybeany.log.collector.service.scan.importer;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.LogLoaderService;
import com.soybeany.log.collector.service.common.model.FileRange;
import com.soybeany.log.collector.service.common.model.ILogReceiver;
import com.soybeany.log.collector.service.common.model.LogIndexes;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

/**
 * @author Soybeany
 * @date 2021/1/15
 */
public interface IndexesImporter {

    void executeImport(LogIndexes indexes) throws IOException;

}

@Component
class IndexesImporterImpl implements IndexesImporter {

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private LogLoaderService logLoaderService;

    @Override
    public void executeImport(LogIndexes indexes) throws IOException {
        logLoaderService.load(indexes.logFile,
                Collections.singletonList(new FileRange(indexes.scannedBytes, Long.MAX_VALUE)),
                new LogReceiver(indexes));
    }

    // ********************内部类********************

    private class LogReceiver implements ILogReceiver {
        private final LogIndexes indexes;
        private final int timeEndIndex;

        public LogReceiver(LogIndexes indexes) {
            this.indexes = indexes;
            this.timeEndIndex = appConfig.lineTimeFormat.indexOf(":s");
        }

        @Override
        public void onReceiveLogLine(long fromByte, long toByte, LogLine logLine) {
            handleTime(fromByte, logLine);
        }

        @Override
        public void onReceiveLogTag(long fromByte, long toByte, LogLine logLine, LogTag logTag) {
            handleTime(fromByte, logLine);
            handleTag(fromByte, toByte, logTag);
        }

        @Override
        public void onFinish(long bytesRead, long endPointer) {
            indexes.scannedBytes = endPointer;
        }

        private void handleTag(long fromByte, long toByte, LogTag logTag) {
            if (!appConfig.tagsToIndex.contains(logTag.key)) {
                return;
            }
        }

        private void handleTime(long fromByte, LogLine logLine) {
            String time = logLine.time;
            Long fByte = indexes.timeIndexMap.get(time);
            // 记录不存在，或者新的记录值更小(兼容模式下，由于有临时列表所以可能非升序)，则进行赋值
            if (null == fByte || fromByte < fByte) {
                indexes.timeIndexMap.put(time, fromByte);
            }
        }
    }
}
