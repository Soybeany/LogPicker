package com.soybeany.log.collector.service.common;

import com.soybeany.config.BDCipherUtils;
import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.common.model.loader.SimpleLogLineLoader;
import com.soybeany.log.collector.service.query.model.LogPackLoader;
import com.soybeany.log.core.model.*;
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
    Map<String, String> getTreatedTagMap(@NonNull Map<String, String> tags);

}

@Service
class LogIndexServiceImpl implements LogIndexService {

    @Autowired
    private AppConfig appConfig;
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
        SimpleLogLineLoader lineLoader = new SimpleLogLineLoader(indexes.logFile, appConfig.logCharset, appConfig.lineParsePattern, appConfig.tagParsePattern);
        lineLoader.seek(indexes.scannedBytes);
        LogPackLoader packLoader = new LogPackLoader(lineLoader, appConfig.maxLinesPerResultWithNullUid, indexes.uidMap);
        packLoader.setListener(holder -> indexTime(indexes, holder.fromByte, holder.logLine));
        LogPack logPack;
        while (null != (logPack = packLoader.loadNextCompleteLogPack())) {
            for (LogTag tag : logPack.tags) {
                indexTag(indexes, tag, logPack.ranges);
            }
        }
        indexes.scannedBytes = lineLoader.getReadPointer();
        // 保存并返回索引
        saveIndexes(indexes);
        return indexes;
    }

    @Override
    public Map<String, String> getTreatedTagMap(Map<String, String> tags) {
        Map<String, String> result = new HashMap<>();
        // 将tag的值转成小写
        tags.forEach((k, v) -> result.put(k, v.toLowerCase()));
        return result;
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
            tempFile.renameTo(indexFile);
        } finally {
            tempFile.delete();
        }
    }

    private void indexTag(LogIndexes indexes, LogTag logTag, List<FileRange> ranges) {
        if (!appConfig.tagsToIndex.contains(logTag.key)) {
            return;
        }
        // 将tag的值转成小写再保存
        LinkedList<FileRange> totalRanges = indexes.tagsIndexMap.computeIfAbsent(logTag.key, k -> new HashMap<>())
                .computeIfAbsent(logTag.value.toLowerCase(), k -> new LinkedList<>());
        totalRanges.addAll(ranges);
    }

    private void indexTime(LogIndexes indexes, long fromByte, LogLine logLine) {
        indexes.timeIndexMap.putIfAbsent(logLine.time, fromByte);
    }

    private File getLogIndexesFile(File logFile) {
        try {
            String md5 = BDCipherUtils.calculateMd5(logFile.getAbsolutePath());
            String fileName = logFile.getName() + " - " + md5.substring(0, 8);
            return new File(appConfig.dirForIndexes, fileName);
        } catch (Exception e) {
            throw new LogException(e);
        }
    }

}
