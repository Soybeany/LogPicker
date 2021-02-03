package com.soybeany.log.collector.service.common;

import com.soybeany.config.BDCipherUtils;
import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.common.model.MsgRecorder;
import com.soybeany.log.collector.service.common.model.loader.LogPackLoader;
import com.soybeany.log.collector.service.common.model.loader.SimpleLogLineLoader;
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
    LogIndexes getIndexes(MsgRecorder recorder, File file) throws IOException;

    @NonNull
    LogIndexes updateAndGetIndexes(MsgRecorder recorder, File file) throws IOException;

    void stabilize(LogIndexes indexes);

    @NonNull
    Map<String, String> getTreatedTagMap(@NonNull Map<String, String> tags);

}

@Service
class LogIndexServiceImpl implements LogIndexService {

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private RangeService rangeService;

    @Override
    public LogIndexes getIndexes(MsgRecorder recorder, File file) throws IOException {
        File indexFile = getLogIndexesFile(file);
        if (!indexFile.exists()) {
            return null;
        }
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(indexFile))) {
            return (LogIndexes) is.readObject();
        } catch (IOException | ClassNotFoundException ignored) {
        }
        // 后续为异常时的处理
        boolean deleted = indexFile.delete();
        recorder.write("“" + file.getName() + "”的异常索引文件，删除" + (deleted ? "成功" : "失败"));
        if (!deleted) {
            throw new IOException("无法删除索引文件“" + indexFile.getName() + "”");
        }
        return null;
    }

    @Override
    public LogIndexes updateAndGetIndexes(MsgRecorder recorder, File file) throws IOException {
        // 得到索引
        LogIndexes indexes = Optional.ofNullable(getIndexes(recorder, file)).orElseGet(() -> new LogIndexes(file));
        long startByte = indexes.scannedBytes;
        // 若索引已是最新，则不再更新
        if (file.length() == startByte) {
            recorder.write("“" + file.getName() + "”的索引不需更新(" + startByte + ")");
            return indexes;
        }
        // 更新索引
        long startTime = System.currentTimeMillis();
        SimpleLogLineLoader lineLoader = new SimpleLogLineLoader(indexes.logFile, appConfig.logCharset, appConfig.lineParsePattern, appConfig.tagParsePattern);
        lineLoader.resetTo(startByte, null); // 因为不会有旧数据，理论上这里不会null异常
        LogPackLoader packLoader = new LogPackLoader(lineLoader, appConfig.maxLinesPerResultWithNullUid, indexes.uidTempMap);
        packLoader.setListener(holder -> indexTime(indexes, holder.fromByte, holder.logLine));
        LogPack logPack;
        while (null != (logPack = packLoader.loadNextCompleteLogPack())) {
            indexTagAndUid(indexes, logPack, true);
        }
        indexes.scannedBytes = lineLoader.getReadPointer();
        long spendTime = System.currentTimeMillis() - startTime;
        recorder.write("“" + file.getName() + "”的索引已更新(" + startByte + "~" + indexes.scannedBytes + ")，耗时" + spendTime + "ms");
        // 保存并返回索引
        saveIndexes(indexes);
        return indexes;
    }

    @Override
    public void stabilize(LogIndexes indexes) {
        Iterator<LogPack> iterator = indexes.uidTempMap.values().iterator();
        while (iterator.hasNext()) {
            LogPack logPack = iterator.next();
            iterator.remove();
            indexTagAndUid(indexes, logPack, false);
        }
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

    private void indexTagAndUid(LogIndexes indexes, LogPack logPack, boolean append) {
        for (LogTag tag : logPack.tags) {
            indexTag(indexes, tag);
        }
        indexUid(indexes, logPack, append);
    }

    private void indexTag(LogIndexes indexes, LogTag logTag) {
        if (!appConfig.tagsToIndex.contains(logTag.key)) {
            return;
        }
        // 将tag的值转成小写，并保存到tagUidMap中
        Set<String> totalUidList = indexes.tagUidMap.computeIfAbsent(logTag.key, k -> new HashMap<>())
                .computeIfAbsent(logTag.value.toLowerCase(), k -> new HashSet<>());
        totalUidList.add(logTag.uid);
    }

    private void indexUid(LogIndexes indexes, LogPack logPack, boolean append) {
        LinkedList<FileRange> totalRanges = indexes.uidRanges.computeIfAbsent(logPack.uid, k -> new LinkedList<>());
        if (append) {
            for (FileRange range : logPack.ranges) {
                rangeService.append(totalRanges, range.from, range.to);
            }
        } else {
            totalRanges.addAll(logPack.ranges);
            LinkedList<FileRange> newRanges = rangeService.merge(totalRanges);
            indexes.uidRanges.put(logPack.uid, newRanges);
        }
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
