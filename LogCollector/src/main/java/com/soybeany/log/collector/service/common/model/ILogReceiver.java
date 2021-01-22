package com.soybeany.log.collector.service.common.model;

import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;

/**
 * @author Soybeany
 * @date 2021/1/15
 */
public interface ILogReceiver {

    int STATE_CONTINUE = 0;
    int STATE_ABORT = 1;

    default void onStart() {
    }

    default void onFinish(long bytesRead, long actualEndPointer) {
    }

    default void onReceiveLogLine(long fromByte, long toByte, LogLine logLine) {
    }

    default void onReceiveLogTag(long fromByte, long toByte, LogLine logLine, LogTag logTag) {
    }

    default int onGetState() {
        return STATE_CONTINUE;
    }

}
