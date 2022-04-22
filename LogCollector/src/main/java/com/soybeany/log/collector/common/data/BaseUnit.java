package com.soybeany.log.collector.common.data;

import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Soybeany
 * @date 2022/4/22
 */
public class BaseUnit implements Serializable {

    public final Lock lock = new ReentrantLock();

    /**
     * 日志文件
     */
    public final File logFile;

    /**
     * 已扫描的字节数
     */
    public long scannedBytes;

    /**
     * 使用uid作为key，存放未组装完成的临时记录
     */
    public final Map<String, LogPack> uidTempMap = new HashMap<>();

    // ********************验证是否还能复用********************

    /**
     * 生成索引时的配置项转换成的md5
     */
    public final String configMd5;

    /**
     * 首行文本
     */
    public final String firstLineText;

    // ********************静态方法区********************

    private static String getConfigMd5(LogCollectConfig config) {
        try {
            return config.getConfigMd5();
        } catch (Exception e) {
            throw new LogException("getConfigMd5：" + e.getMessage());
        }
    }

    private static String readFirstLine(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (null == line || line.isEmpty()) {
                throw new LogException("不支持对空白文件进行索引");
            }
            return line;
        } catch (Exception e) {
            throw new LogException("readFirstLine:" + e.getMessage());
        }
    }

    // ********************普通方法区********************

    public BaseUnit(LogCollectConfig logCollectConfig, File logFile) {
        this.logFile = logFile;
        this.configMd5 = getConfigMd5(logCollectConfig);
        this.firstLineText = readFirstLine(logFile);
    }

    /**
     * 检查索引是否依然有效，因为配置变化、当天日志文件滚动等情况，索引文件将不再有效
     */
    public void check(LogCollectConfig logCollectConfig) {
        String curConfigMd5 = getConfigMd5(logCollectConfig);
        if (!configMd5.equals(curConfigMd5)) {
            throw new LogException("配置发生了变更");
        }
        if (logFile.length() < scannedBytes) {
            throw new LogException("文件变小了");
        }
        String curFirstLineText = readFirstLine(logFile);
        if (!firstLineText.equals(curFirstLineText)) {
            throw new LogException("文件内容发生了变更");
        }
    }

}
