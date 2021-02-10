package com.soybeany.log.collector.common.model.loader;

import com.soybeany.log.core.model.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/24
 */
public class LogPackLoader implements Closeable {

    private final ILogLineLoader logLineLoader;
    private final String noUidPlaceholder;
    private final int maxLinesPerResultWithNoUid;
    private Map<String, LogPack> uidMap;

    private final ILogLineLoader.ResultHolder holder = new SimpleLogLineLoader.ResultHolder();
    private IListener listener;

    public LogPackLoader(ILogLineLoader logLineLoader, String noUidPlaceholder, int maxLinesPerResultWithNoUid, Map<String, LogPack> uidMap) {
        this.logLineLoader = logLineLoader;
        this.noUidPlaceholder = noUidPlaceholder;
        this.maxLinesPerResultWithNoUid = maxLinesPerResultWithNoUid;
        switchUidMap(uidMap);
    }

    @Override
    public void close() throws IOException {
        logLineLoader.close();
    }

    public void switchUidMap(Map<String, LogPack> uidMap) {
        this.uidMap = uidMap;
    }

    public void setListener(IListener listener) {
        this.listener = listener;
    }

    public ILogLineLoader getLogLineLoader() {
        return logLineLoader;
    }

    /**
     * @return null表示已没有更多完整的logPack
     */
    public LogPack loadNextCompleteLogPack() throws IOException {
        while (logLineLoader.loadNextLogLine(holder)) {
            LogLine logLine = holder.logLine;
            String uidMapKey = getUidMapKey(logLine.uid, logLine.thread);
            LogPack logPack = getLogPack(uidMapKey, logLine.uid, logLine.thread);
            boolean isComplete;
            // 处理标签
            if (holder.isTag) {
                isComplete = handleTag(uidMapKey, logPack, holder.logTag);
            }
            // 处理行
            else {
                isComplete = handleLine(uidMapKey, logPack, logLine);
            }
            // 添加下标
            logPack.ranges.add(new FileRange(holder.fromByte, holder.toByte));
            // 执行监听器回调
            if (null != listener) {
                listener.onReadLine(holder);
            }
            // 若可返回结果则返回结果
            if (isComplete) {
                return logPack;
            }
        }
        return null;
    }

    // ********************内部方法********************

    private boolean handleTag(String uidMapKey, LogPack logPack, LogTag logTag) {
        switch (logTag.key) {
            case Constants.TAG_BORDER_START:
                logPack.startTag = logTag;
                break;
            case Constants.TAG_BORDER_END:
                logPack.endTag = logTag;
                uidMap.remove(uidMapKey);
                return true;
            default:
                logPack.tags.add(logTag);
        }
        return false;
    }

    private boolean handleLine(String uidMapKey, LogPack logPack, LogLine logLine) {
        boolean isComplete = (noUidPlaceholder.equals(logLine.uid) && logPack.logLines.size() + 1 > maxLinesPerResultWithNoUid);
        if (isComplete) {
            uidMap.remove(uidMapKey);
            logPack = getLogPack(uidMapKey, logLine.uid, logLine.thread);
        }
        logPack.logLines.add(logLine);
        return isComplete;
    }

    private LogPack getLogPack(String uidMapKey, String uid, String thread) {
        return uidMap.computeIfAbsent(uidMapKey, k -> getNewLogPack(uid, thread));
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

    // ********************内部类********************

    public interface IListener {
        void onReadLine(ILogLineLoader.ResultHolder holder);
    }

}
