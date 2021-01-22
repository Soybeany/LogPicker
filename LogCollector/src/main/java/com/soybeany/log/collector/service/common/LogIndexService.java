package com.soybeany.log.collector.service.common;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.model.FileRange;
import com.soybeany.log.collector.service.common.model.ILogReceiver;
import com.soybeany.log.collector.service.common.model.LogIndexes;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import com.soybeany.util.file.BdFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/14
 */
public interface LogIndexService {

    @Nullable
    LogIndexes getIndexes(File file) throws IOException, ClassNotFoundException;

    @NonNull
    LogIndexes updateAndGetIndexes(File file) throws IOException, ClassNotFoundException;

    @NonNull
    List<FileRange> getRangesOfTag(LogIndexes indexes, String tagKey, String tagValue);

}

@Service
class LogIndexServiceImpl implements LogIndexService {

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private LogLoaderService logLoaderService;
    @Autowired
    private BytesRangeService bytesRangeService;

    @Override
    public LogIndexes getIndexes(File file) throws IOException, ClassNotFoundException {
        File indexFile = getLogIndexesFile(file);
        if (!indexFile.exists()) {
            return null;
        }
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(indexFile))) {
            return (LogIndexes) is.readObject();
        }
    }

    @Override
    public LogIndexes updateAndGetIndexes(File file) throws IOException, ClassNotFoundException {
        // 得到索引
        LogIndexes indexes = Optional.ofNullable(getIndexes(file)).orElseGet(() -> new LogIndexes(file));
        // 若索引已是最新，则不再更新
        if (file.length() == indexes.scannedBytes) {
            return indexes;
        }
        // 更新索引
        logLoaderService.load(indexes.logFile,
                Collections.singletonList(new FileRange(indexes.scannedBytes, Long.MAX_VALUE)),
                new LogReceiver(indexes));
        // 保存并返回索引
        saveIndexes(indexes);
        return indexes;
    }

    @Override
    public List<FileRange> getRangesOfTag(LogIndexes indexes, String tagKey, String tagValue) {
        List<FileRange> ranges = new LinkedList<>();
        indexes.tagsIndexMap.get(tagKey).forEach((value, r) -> {
            if (value.contains(tagValue.toLowerCase())) {
                ranges.addAll(r);
            }
        });
        return ranges;
    }

    // ********************内部方法********************

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveIndexes(LogIndexes indexes) throws IOException {
        File tempFile = new File(appConfig.dirForIndexes + "/temp", UUID.randomUUID().toString());
        BdFileUtils.mkParentDirs(tempFile);
        // 将索引保存到临时文件
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(tempFile))) {
            os.writeObject(indexes);
        }
        // 替换原文件
        try {
            File indexFile = getLogIndexesFile(indexes.logFile);
            indexFile.delete();
            BdFileUtils.mkParentDirs(indexFile);
            boolean success = tempFile.renameTo(indexFile);
            System.out.println("索引持久化:" + success);
        } finally {
            tempFile.delete();
        }
    }

    private File getLogIndexesFile(File logFile) {
        String logFilePath = logFile.getAbsolutePath();
        return new File(appConfig.dirForIndexes, logFilePath.replaceAll(":", ""));
    }

    // ********************内部类********************

    private class LogReceiver implements ILogReceiver {
        private final LogIndexes indexes;

        public LogReceiver(LogIndexes indexes) {
            this.indexes = indexes;
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
        public void onFinish(long bytesRead, long actualEndPointer) {
            indexes.scannedBytes = actualEndPointer;
        }

        private void handleTag(long fromByte, long toByte, LogTag logTag) {
            if (!appConfig.tagsToIndex.contains(logTag.key)) {
                return;
            }
            // 将tag的值转成小写再保存
            LinkedList<FileRange> ranges = indexes.tagsIndexMap.computeIfAbsent(logTag.key, k -> new HashMap<>())
                    .computeIfAbsent(logTag.value.toLowerCase(), k -> new LinkedList<>());
            bytesRangeService.append(ranges, fromByte, toByte);
        }

        private void handleTime(long fromByte, LogLine logLine) {
            indexes.timeIndexMap.putIfAbsent(logLine.time, fromByte);
        }
    }
}
