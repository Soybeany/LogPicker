package com.soybeany.log.collector.service.scan.parser;

import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import com.soybeany.log.core.util.UidUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.soybeany.log.core.model.Constants.*;

/**
 * @author Soybeany
 * @date 2021/1/12
 */
@Scope("prototype")
@Component
public class CompactLogParser implements LogParser {

    private static final Pattern TAG_PATTERN = Pattern.compile("FLAG-(?<type>.+?)-(?<state>.+?):(?<user>.+?) (?<url>.+?) (?<param>\\{.+})");

    private final Map<String, String> uidMap = new HashMap<>();

    @Override
    public LogLine parseToLogLine(Pattern pattern, DateFormat dateFormat, String lineString) {
        Matcher matcher = pattern.matcher(lineString);
        if (!matcher.find()) {
            return null;
        }
        LogLine line = new LogLine();
        try {
            line.time = dateFormat.parse(matcher.group(PARSER_KEY_TIME));
        } catch (ParseException e) {
            throw new LogException("时间解析异常:" + e.getMessage());
        }
        line.thread = matcher.group(PARSER_KEY_THREAD);
        line.level = matcher.group(PARSER_KEY_LEVEL);
        line.content = matcher.group(PARSER_KEY_CONTENT);
        line.uid = uidMap.get(line.thread);
        return line;
    }

    @Override
    public List<LogTag> parseToLogTags(LogLine logLine) {
        Matcher matcher = TAG_PATTERN.matcher(logLine.content);
        if (!matcher.find()) {
            return null;
        }
        List<LogTag> result = new LinkedList<>();
        String state = matcher.group("state");
        String uid;
        switch (state) {
            case "开始":
                uid = UidUtils.getNew();
                uidMap.put(logLine.thread, uid);
                addTagsToList(result, uid, logLine, matcher);
                result.add(getNewLogTag(logLine, uid, TAG_BORDER_START, null));
                break;
            case "结束":
                uid = uidMap.remove(logLine.thread);
                if (null == uid) {
                    uid = UidUtils.getNew();
                    addTagsToList(result, uid, logLine, matcher);
                }
                result.add(getNewLogTag(logLine, uid, TAG_BORDER_END, null));
                break;
            default:
                throw new LogException("使用了未知的状态");
        }
        return result;
    }

    private void addTagsToList(List<LogTag> list, String uid, LogLine logLine, Matcher matcher) {
        list.add(getNewLogTag(logLine, uid, matcher, "user"));
        list.add(getNewLogTag(logLine, uid, matcher, "url"));
        list.add(getNewLogTag(logLine, uid, matcher, "param"));
    }

    private LogTag getNewLogTag(LogLine logLine, String uid, Matcher matcher, String key) {
        return getNewLogTag(logLine, uid, key, matcher.group(key));
    }

    private LogTag getNewLogTag(LogLine logLine, String uid, String key, String value) {
        LogTag tag = new LogTag();
        tag.thread = logLine.thread;
        tag.time = logLine.time;
        tag.uid = uid;
        tag.key = key;
        tag.value = value;
        return tag;
    }

}
