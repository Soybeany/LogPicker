package com.soybeany.log.collector.service.scan.parser;

import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public interface TagParser {

    /**
     * @return 能够解析返回对象，不能解析返回null
     */
    LogTag parse(LogLine logLine);

}

@Scope("prototype")
@Component
class TagParserImpl implements TagParser {

    private final Pattern tagPattern = Pattern.compile("FLAG-(?<key>.+)-(?<value>.*)");

    @Override
    public LogTag parse(LogLine logLine) {
        Matcher matcher = tagPattern.matcher(logLine.content);
        if (!matcher.find()) {
            return null;
        }
        LogTag tag = new LogTag();
        tag.uid = logLine.uid;
        tag.thread = logLine.thread;
        tag.time = logLine.time;
        tag.key = matcher.group("key");
        tag.value = matcher.group("value");
        return tag;
    }
}
