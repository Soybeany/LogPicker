package com.soybeany.log.collector.service.common;

import com.soybeany.log.collector.service.common.model.ILogReceiver;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.soybeany.log.core.model.Constants.*;

/**
 * @author Soybeany
 * @date 2021/1/12
 */
public interface LogParserService {

    /**
     * 批量解析前的回调
     */
    default void beforeBatchParse(ILogReceiver receiver) {
    }

    /**
     * 批量解析后的回调
     */
    default void afterBatchParse(ILogReceiver receiver) {
    }

    /**
     * 解析单行时的回调
     */
    void onParse(Pattern pattern, long fromByte, long toByte, String lineString, ILogReceiver receiver);

}

/**
 * @author Soybeany
 * @date 2021/1/15
 */
@Service
class LogParserServiceImpl implements LogParserService {

    private static final Pattern TAG_PATTERN = Pattern.compile("^FLAG-(?<" + PARSER_KEY_KEY + ">.+?)-(?<" + PARSER_KEY_VALUE + ">.*)");

    private static final ThreadLocal<LastLogLineHolder> HOLDER = new ThreadLocal<>();

    @Override
    public void beforeBatchParse(ILogReceiver receiver) {
        HOLDER.set(new LastLogLineHolder());
    }

    @Override
    public void afterBatchParse(ILogReceiver receiver) {
        handleLastLogLine(receiver);
        HOLDER.remove();
    }

    @Override
    public void onParse(Pattern pattern, long fromByte, long toByte, String lineString, ILogReceiver receiver) {
        LogLine curLogLine = parseToLogLine(pattern, lineString);
        // 尝试日志拼接
        LastLogLineHolder holder = HOLDER.get();
        if (null == curLogLine) {
            // 舍弃无法拼接的日志
            if (null == holder.logLine) {
                return;
            }
            if (null == holder.tempContent) {
                holder.tempContent = new StringBuilder(holder.logLine.content);
            }
            holder.tempContent.append(lineString);
            holder.toByte = toByte;
            return;
        }
        // 处理lastLogLine
        handleLastLogLine(receiver);
        // 将lastLogLine替换为当前LogLine
        holder.logLine = curLogLine;
        holder.fromByte = fromByte;
        holder.toByte = toByte;
    }

    // ********************内部方法********************

    private void handleLastLogLine(ILogReceiver receiver) {
        LastLogLineHolder holder = HOLDER.get();
        if (null == holder.logLine) {
            return;
        }
        // 若有必要，进行日志替换
        if (null != holder.tempContent) {
            holder.logLine.content = holder.tempContent.toString();
            holder.tempContent = null;
        }
        // 发送日志
        sendLogToReceiver(receiver, holder.fromByte, holder.toByte, holder.logLine);
    }

    private void sendLogToReceiver(ILogReceiver receiver, long fromByte, long toByte, LogLine logLine) {
        LogTag logTag = parseToLogTag(logLine);
        if (null != logTag) {
            receiver.onReceiveLogTag(fromByte, toByte, logLine, logTag);
        } else {
            receiver.onReceiveLogLine(fromByte, toByte, logLine);
        }
    }

    private LogLine parseToLogLine(Pattern pattern, String lineString) {
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

    private LogTag parseToLogTag(LogLine logLine) {
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
        return tag;
    }

    // ********************内部类********************

    private static class LastLogLineHolder {
        LogLine logLine;
        StringBuilder tempContent;
        long fromByte;
        long toByte;
    }

}
