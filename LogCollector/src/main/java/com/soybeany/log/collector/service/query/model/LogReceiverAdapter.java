package com.soybeany.log.collector.service.query.model;

import com.soybeany.log.collector.service.common.model.ILogReceiver;
import com.soybeany.log.core.model.Constants;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.model.LogTag;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/20
 */
public class LogReceiverAdapter implements ILogReceiver {

    private final int maxLinesPerResultWithNullUid;
    private final Map<String, LogPack> uidMap;
    private final ILogPackReceiver receiver;

    private int status = STATE_CONTINUE;

    public LogReceiverAdapter(int maxLinesPerResultWithNullUid, Map<String, LogPack> uidMap, ILogPackReceiver receiver) {
        this.maxLinesPerResultWithNullUid = maxLinesPerResultWithNullUid;
        this.uidMap = uidMap;
        this.receiver = receiver;
    }

    @Override
    public void onStart() {
        receiver.onStart();
    }

    @Override
    public void onFinish(long bytesRead, long actualEndPointer) {
        for (LogPack logPack : uidMap.values()) {
            invokeOnReceive(logPack);
        }
        receiver.onFinish(bytesRead, actualEndPointer);
    }

    @Override
    public void onReceiveLogLine(long fromByte, long toByte, LogLine logLine) {
        LogPack logPack = getLogPackHolder(logLine.uid, logLine.thread);
        // 若是无uid的pack且其包含的行数已达指定数目，则先发送
        boolean needSend = null == logLine.uid && logPack.logLines.size() + 1 > maxLinesPerResultWithNullUid;
        if (needSend) {
            invokeOnReceive(logPack);
            uidMap.remove(getUidMapKey(logLine.uid, logLine.thread));
            logPack = getLogPackHolder(logLine.uid, logLine.thread);
        }
        logPack.logLines.add(logLine);
    }

    @Override
    public void onReceiveLogTag(long fromByte, long toByte, LogLine logLine, LogTag logTag) {
        LogPack logPack = getLogPackHolder(logTag.uid, logTag.thread);
        switch (logTag.key) {
            case Constants.TAG_BORDER_START:
                logPack.startTag = logTag;
                break;
            case Constants.TAG_BORDER_END:
                logPack.endTag = logTag;
                invokeOnReceive(logPack);
                break;
            default:
                logPack.tags.add(logTag);
        }
    }

    @Override
    public int onGetState() {
        return status;
    }

    // ********************内部方法********************

    private LogPack getLogPackHolder(String uid, String thread) {
        return uidMap.computeIfAbsent(getUidMapKey(uid, thread), k -> getNewLogPack(uid, thread));
    }

    private void invokeOnReceive(LogPack logPack) {
        status = receiver.onReceive(logPack) ? STATE_CONTINUE : STATE_ABORT;
    }

    private String getUidMapKey(String uid, String thread) {
        return uid + "-" + thread;
    }

    private LogPack getNewLogPack(String uid, String thread) {
        LogPack logPack = new LogPack();
        logPack.uid = uid;
        logPack.thread = thread;
        return logPack;
    }
}
