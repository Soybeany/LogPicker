package com.soybeany.log.collector.service.common.model;

import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;

/**
 * @author Soybeany
 * @date 2021/1/15
 */
public interface ILogReceiver {

    default void onStart() {
    }

    default void onFinish(long bytesRead, long endPointer) {
    }

    void onReceiveLogLine(long fromByte, long toByte, LogLine logLine);

    void onReceiveLogTag(long fromByte, long toByte, LogLine logLine, LogTag logTag);
}
