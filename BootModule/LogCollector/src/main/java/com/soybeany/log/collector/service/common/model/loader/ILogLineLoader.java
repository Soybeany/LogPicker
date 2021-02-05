package com.soybeany.log.collector.service.common.model.loader;

import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Soybeany
 * @date 2021/1/25
 */
public interface ILogLineLoader extends Closeable {

    boolean loadNextLogLine(ResultHolder resultHolder) throws IOException;

    long getReadPointer();

    long getReadBytes();

    class ResultHolder {
        public long fromByte;
        public long toByte;
        public boolean isTag;
        public LogLine logLine;
        public LogTag logTag;
    }

}
