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

    private final LogIndexService logIndexService;

    public ScanService(LogCollectConfig logCollectConfig) {
        RangeService rangeService = new RangeService(logCollectConfig);
        this.logIndexService = new LogIndexService(logCollectConfig, rangeService);
    }

    public LogIndexes updateAndGet(MsgRecorder recorder, File logFile) {
        return scanFile(recorder, logFile);
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