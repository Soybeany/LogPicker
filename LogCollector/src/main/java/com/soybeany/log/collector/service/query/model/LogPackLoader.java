package com.soybeany.log.collector.service.query.model;

import com.soybeany.log.collector.service.common.model.loader.ILogLineLoader;
import com.soybeany.log.collector.service.common.model.loader.SimpleLogLineLoader;
import com.soybeany.log.core.model.Constants;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.model.LogTag;
import org.springframework.lang.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/24
 */
public class LogPackLoader implements Closeable {

    private final ILogLineLoader logLineLoader;
    private final int maxLinesPerResultWithNullUid;
    private final Map<String, LogPack> uidMap;

    private final ILogLineLoader.ResultHolder holder = new SimpleLogLineLoader.ResultHolder();

    public LogPackLoader(ILogLineLoader logLineLoader, int maxLinesPerResultWithNullUid, Map<String, LogPack> uidMap) {
        this.logLineLoader = logLineLoader;
        this.maxLinesPerResultWithNullUid = maxLinesPerResultWithNullUid;
        this.uidMap = uidMap;
    }

    @Override
    public void close() throws IOException {
        logLineLoader.close();
    }

    /**
     * @return null表示已没有更多完整的logPack
     */
    @Nullable
    public LogPack loadNextCompleteLogPack() throws IOException {
        while (logLineLoader.loadNextLogLine(holder)) {
            LogLine logLine = holder.logLine;
            LogPack logPack = getLogPack(logLine.uid, logLine.thread);
            boolean isComplete;
            // 处理标签
            if (holder.isTag) {
                isComplete = handleTag(logPack, holder.logTag);
            }
            // 处理行
            else {
                isComplete = handleLine(logPack, logLine);
            }
            // 若可返回结果则返回结果
            if (isComplete) {
                return logPack;
            }
        }
        return null;
    }

    // ********************内部方法********************

    private boolean handleTag(LogPack logPack, LogTag logTag) {
        switch (logTag.key) {
            case Constants.TAG_BORDER_START:
                logPack.startTag = logTag;
                break;
            case Constants.TAG_BORDER_END:
                logPack.endTag = logTag;
                return true;
            default:
                logPack.tags.add(logTag);
        }
        return false;
    }

    private boolean handleLine(LogPack logPack, LogLine logLine) {
        boolean isComplete = (null == logLine.uid && logPack.logLines.size() + 1 > maxLinesPerResultWithNullUid);
        if (isComplete) {
            logPack = removeAndGetNewLogPack(logLine.uid, logLine.thread);
        }
        logPack.logLines.add(logLine);
        return isComplete;
    }

    private LogPack getLogPack(String uid, String thread) {
        return uidMap.computeIfAbsent(getUidMapKey(uid, thread), k -> getNewLogPack(uid, thread));
    }

    private LogPack removeAndGetNewLogPack(String uid, String thread) {
        uidMap.remove(getUidMapKey(uid, thread));
        return getLogPack(uid, thread);
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
