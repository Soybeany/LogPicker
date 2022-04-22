package com.soybeany.log.collector.common.model.loader;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Soybeany
 * @date 2021/1/25
 */
public interface ILogLineLoader extends Closeable {

    boolean loadNextLogLine(LogLineHolder resultHolder) throws IOException;

    long getReadPointer();

    long getReadBytes();

}
