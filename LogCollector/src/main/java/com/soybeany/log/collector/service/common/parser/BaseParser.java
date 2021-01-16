package com.soybeany.log.collector.service.common.parser;

import com.soybeany.log.collector.service.common.model.ILogReceiver;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/15
 */
public abstract class BaseParser implements LogParser {

    private static final ThreadLocal<LastLogLineHolder> HOLDER = new ThreadLocal<>();

    @Override
    public void beforeBatchParse() {
        HOLDER.set(new LastLogLineHolder());
    }

    @Override
    public void afterBatchParse(ILogReceiver receiver) {
        handleLastLogLine(true, receiver);
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
        handleLastLogLine(false, receiver);
        // 将lastLogLine替换为当前LogLine
        holder.logLine = curLogLine;
        holder.fromByte = fromByte;
        holder.toByte = toByte;
    }

    // ********************子类回调********************

    protected void onSendLogLine(boolean isFinal, ILogReceiver receiver, long fromByte, long toByte, LogLine logLine) {
        receiver.onReceiveLogLine(fromByte, toByte, logLine);
    }

    protected void onSendLogTag(boolean isFinal, ILogReceiver receiver, long fromByte, long toByte, LogLine logLine, List<LogTag> logTags) {
        for (LogTag logTag : logTags) {
            receiver.onReceiveLogTag(fromByte, toByte, logLine, logTag);
        }
    }

    // ********************内部方法********************

    private void handleLastLogLine(boolean isFinal, ILogReceiver receiver) {
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
        sendLogToReceiver(isFinal, receiver, holder.fromByte, holder.toByte, holder.logLine);
    }

    private void sendLogToReceiver(boolean isFinal, ILogReceiver receiver, long fromByte, long toByte, LogLine logLine) {
        List<LogTag> tags = parseToLogTags(logLine);
        if (null != tags) {
            onSendLogTag(isFinal, receiver, fromByte, toByte, logLine, tags);
        } else {
            onSendLogLine(isFinal, receiver, fromByte, toByte, logLine);
        }
    }

    // ********************抽象方法********************

    protected abstract LogLine parseToLogLine(Pattern pattern, String lineString);

    protected abstract List<LogTag> parseToLogTags(LogLine logLine);

    // ********************内部类********************

    private static class LastLogLineHolder {
        LogLine logLine;
        StringBuilder tempContent;
        long fromByte;
        long toByte;
    }

}
