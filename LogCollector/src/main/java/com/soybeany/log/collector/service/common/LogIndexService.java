package com.soybeany.log.collector.service.common;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.model.LogIndexes;
import com.soybeany.util.file.BdFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.UUID;

/**
 * @author Soybeany
 * @date 2021/1/14
 */
public interface LogIndexService {

    @Nullable
    LogIndexes loadIndexes(File file) throws IOException, ClassNotFoundException;

    void saveIndexes(File file, LogIndexes indexes) throws IOException;
}

@Service
class LogIndexServiceImpl implements LogIndexService {

    @Autowired
    private AppConfig appConfig;

    @Override
    public LogIndexes loadIndexes(File file) throws IOException, ClassNotFoundException {
        File indexFile = getLogIndexesFile(file);
        // 若索引文件不存在，返回null
        if (!indexFile.exists()) {
            return null;
        }
        // 读取索引文件
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))) {
            return (LogIndexes) is.readObject();
        }
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void saveIndexes(File file, LogIndexes indexes) throws IOException {
        File tempFile = new File(appConfig.dirForIndexes + "/temp", UUID.randomUUID().toString());
        BdFileUtils.mkParentDirs(tempFile);
        // 将索引保存到临时文件
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(tempFile))) {
            os.writeObject(indexes);
        }
        // 替换原文件
        try {
            File indexFile = getLogIndexesFile(file);
            indexFile.delete();
            tempFile.renameTo(indexFile);
        } finally {
            tempFile.delete();
        }
    }

    private File getLogIndexesFile(File logFile) {
        String logFilePath = logFile.getAbsolutePath();
        return new File(appConfig.dirForIndexes, logFilePath.replaceAll(":", ""));
    }

}
