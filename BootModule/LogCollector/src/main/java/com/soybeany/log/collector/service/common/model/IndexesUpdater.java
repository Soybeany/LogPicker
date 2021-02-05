package com.soybeany.log.collector.service.common.model;

import com.soybeany.log.collector.service.common.data.LogIndexes;

import java.io.File;

/**
 * @author Soybeany
 * @date 2021/2/2
 */
public interface IndexesUpdater {
    LogIndexes updateAndGet(MsgRecorder recorder, File logFile);
}
