package com.soybeany.log.collector.service.common.parser;

import com.soybeany.log.collector.service.common.model.ILogReceiver;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import com.soybeany.log.core.util.UidUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.soybeany.log.core.model.Constants.*;

/**
 * @author Soybeany
 * @date 2021/1/12
 */
@Component
public class CompactLogParser extends BaseParser {

    private static final Pattern TAG_PATTERN = Pattern.compile("FLAG-(?<type>.+?)-(?<state>.+?):(?<user>.+?) (?<url>.+?) (?<param>\\{.+})");

    private static final ThreadLocal<InfoHolder> HOLDER = new ThreadLocal<>();

    @Override
    public void beforeBatchParse() {
        HOLDER.set(new InfoHolder());
        super.beforeBatchParse();
    }

    @Override
    public void afterBatchParse(ILogReceiver receiver) {
        super.afterBatchParse(receiver);
        HOLDER.remove();
    }

    @Override
    protected void onSendLogLine(boolean isFinal, ILogReceiver receiver, long fromByte, long toByte, LogLine logLine) {
        InfoHolder holder = HOLDER.get();
        String uid = holder.uidMap.get(logLine.thread);
        // 当前行，若能匹配uid，则直接发送；否则放入临时列表
        if (null != uid) {
            logLine.uid = uid;
            super.onSendLogLine(isFinal, receiver, fromByte, toByte, logLine);
        } else {
            holder.threadMap.computeIfAbsent(logLine.thread, k -> new LinkedList<>())
                    .add(new LogLineHolderWithPos(fromByte, toByte, logLine));
        }
        // 若为最终，将临时列表的记录全部发送
        if (isFinal) {
            for (List<LogLineHolderWithPos> lines : holder.threadMap.values()) {
                for (LogLineHolderWithPos lineHolder : lines) {
                    super.onSendLogLine(true, receiver, lineHolder.fromByte, lineHolder.toByte, lineHolder.logLine);
                }
            }
        }
    }

    @Override
    protected void onSendLogTag(boolean isFinal, ILogReceiver receiver, long fromByte, long toByte, LogLine logLine, List<LogTag> logTags) {
        // 若为最终，则不作处理
        if (isFinal) {
            super.onSendLogTag(true, receiver, fromByte, toByte, logLine, logTags);
            return;
        }
        // 处理uid
        InfoHolder holder = HOLDER.get();
        String uid = null;
        out:
        for (LogTag logTag : logTags) {
            switch (logTag.key) {
                case TAG_BORDER_START:
                    uid = getNewUid();
                    holder.uidMap.put(logTag.thread, uid);
                    break out;
                case TAG_BORDER_END:
                    uid = holder.uidMap.remove(logTag.thread);
                    if (null == uid) {
                        uid = getNewUid();
                        String finalUid = uid;
                        Optional.ofNullable(holder.threadMap.remove(logTag.thread))
                                .ifPresent(holders -> holders.forEach(h -> h.logLine.uid = finalUid));
                    }
                    break out;
                default:
            }
        }
        for (LogTag logTag : logTags) {
            logTag.uid = uid;
        }
        super.onSendLogTag(false, receiver, fromByte, toByte, logLine, logTags);
    }

    @Override
    protected LogLine parseToLogLine(Pattern pattern, String lineString) {
        Matcher matcher = pattern.matcher(lineString);
        if (!matcher.find()) {
            return null;
        }
        LogLine line = new LogLine();
        line.time = matcher.group(PARSER_KEY_TIME);
        line.thread = matcher.group(PARSER_KEY_THREAD);
        line.level = matcher.group(PARSER_KEY_LEVEL);
        line.content = matcher.group(PARSER_KEY_CONTENT);
        return line;
    }

    @Override
    protected List<LogTag> parseToLogTags(LogLine logLine) {
        Matcher matcher = TAG_PATTERN.matcher(logLine.content);
        if (!matcher.find()) {
            return null;
        }
        List<LogTag> result = new LinkedList<>();
        switch (matcher.group("state")) {
            case "开始":
                result.add(getNewLogTag(logLine, TAG_BORDER_START, null));
                break;
            case "结束":
                result.add(getNewLogTag(logLine, TAG_BORDER_END, null));
                break;
            default:
                throw new LogException("使用了未知的状态");
        }
        addTagsToList(result, logLine, matcher);
        return result;
    }

    private String getNewUid() {
        return UidUtils.getNew().substring(0, 16);
    }

    private void addTagsToList(List<LogTag> list, LogLine logLine, Matcher matcher) {
        list.add(getNewLogTag(logLine, matcher, "user"));
        list.add(getNewLogTag(logLine, matcher, "url"));
        list.add(getNewLogTag(logLine, matcher, "param"));
    }

    private LogTag getNewLogTag(LogLine logLine, Matcher matcher, String key) {
        return getNewLogTag(logLine, key, matcher.group(key));
    }

    private LogTag getNewLogTag(LogLine logLine, String key, String value) {
        LogTag tag = new LogTag();
        tag.thread = logLine.thread;
        tag.time = logLine.time;
        tag.key = key;
        tag.value = value;
        return tag;
    }

    // ********************内部类********************

    private static class InfoHolder {
        final Map<String, String> uidMap = new HashMap<>();
        final Map<String, List<LogLineHolderWithPos>> threadMap = new HashMap<>();
    }

    private static class LogLineHolderWithPos {
        long fromByte;
        long toByte;
        LogLine logLine;

        public LogLineHolderWithPos(long fromByte, long toByte, LogLine logLine) {
            this.fromByte = fromByte;
            this.toByte = toByte;
            this.logLine = logLine;
        }
    }

}
