package com.soybeany.log.collector.scan;

import com.soybeany.log.collector.common.LogIndexService;
import com.soybeany.log.collector.common.RangeService;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.collector.common.model.MsgRecorder;
import com.soybeany.log.core.model.LogException;

import java.io.File;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class ScanService {

    private final LogCollectConfig logCollectConfig;
    private final LogIndexService logIndexService;

    public ScanService(LogCollectConfig logCollectConfig) {
        this.logCollectConfig = logCollectConfig;
        RangeService rangeService = new RangeService(logCollectConfig);
        this.logIndexService = new LogIndexService(logCollectConfig, rangeService);
    }

    public LogIndexes updateAndGet(MsgRecorder recorder, File logFile) {
        return scanFile(recorder, logFile);
    }

    /**
     * 执行全扫描
     */
    public void fullScan() {
        MsgRecorder recorder = msg -> {
            // todo 修改为写日志
        };
        for (String dir : logCollectConfig.dirsToScan) {
            File[] files = new File(dir).listFiles();
            if (null == files) {
                throw new LogException("指定的日志目录不存在");
            }
            for (File file : files) {
                scanFile(recorder, file);
            }
        }
    }

    /**
     * 扫描指定的文件，多个文件使用“;”进行分隔
     */
    public void scanFiles(String paths) {

    }

    // ********************内部方法********************

    private LogIndexes scanFile(MsgRecorder recorder, File file) {
        try {
            return logIndexService.updateAndGetIndexes(recorder, file);
        } catch (Exception e) {
            throw new LogException("日志文件扫描异常:" + e.getMessage());
        }
    }

}