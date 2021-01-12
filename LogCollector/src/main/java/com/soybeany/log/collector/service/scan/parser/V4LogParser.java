package com.soybeany.log.collector.service.scan.parser;

import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.soybeany.log.core.model.Constants.*;

/**
 * @author Soybeany
 * @date 2021/1/12
 */
@Scope("prototype")
@Component
public class V4LogParser implements LogParser {

    private static final Pattern TAG_PATTERN = Pattern.compile("FLAG-(?<" + PARSER_KEY_KEY + ">.+?)-(?<" + PARSER_KEY_VALUE + ">.*)");

    @Override
    public LogLine parseToLogLine(Pattern pattern, String lineString) {
        Matcher matcher = pattern.matcher(lineString);
        if (!matcher.find()) {
            return null;
        }
        LogLine line = new LogLine();
        line.time = matcher.group(PARSER_KEY_TIME);
        line.uid = matcher.group(PARSER_KEY_UID);
        line.thread = matcher.group(PARSER_KEY_THREAD);
        line.level = matcher.group(PARSER_KEY_LEVEL);
        line.content = matcher.group(PARSER_KEY_CONTENT);
        return line;
    }

    @Override
    public List<LogTag> parseToLogTags(LogLine logLine) {
        Matcher matcher = TAG_PATTERN.matcher(logLine.content);
        if (!matcher.find()) {
            return null;
        }
        LogTag tag = new LogTag();
        tag.uid = logLine.uid;
        tag.thread = logLine.thread;
        tag.time = logLine.time;
        tag.key = matcher.group(PARSER_KEY_KEY);
        tag.value = matcher.group(PARSER_KEY_VALUE);
        return Collections.singletonList(tag);
    }

}
