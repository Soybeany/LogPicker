package com.soybeany.log.collector.common.model.loader;

import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;

/**
 * @author Soybeany
 * @date 2022/4/22
 */
public class LogLineHolder {
    public long fromByte;
    public long toByte;
    public boolean isTag;
    public LogLine logLine;
    public LogTag logTag;
}
