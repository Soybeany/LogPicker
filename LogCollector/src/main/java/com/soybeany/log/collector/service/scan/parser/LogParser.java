package com.soybeany.log.collector.service.scan.parser;

import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/12
 */
public interface LogParser {

    /**
     * @return 能够解析返回对象，不能解析返回null
     */
    @Nullable
    LogLine parseToLogLine(Pattern pattern, String lineString);

    /**
     * @return 是否解析成功
     */
    @Nullable
    List<LogTag> parseToLogTags(LogLine logLine);

}
