package com.soybeany.log.collector.service;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.model.LogLine;
import com.soybeany.log.collector.model.LogPack;
import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.repository.TagInfo;
import com.soybeany.log.collector.repository.TagInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/10
 */
public interface LogCutterService {

    List<LogPack> cut(QueryContext context, List<LogLine> lines);
}

@Service
class LogCutterServiceImpl implements LogCutterService {


    private static final String PREFIX = "logCutter";

    private static final String P_KEY_MAX_LINES_PER_RESULT_WITH_NULL_UID = "maxLinesPerResultWithNullUid"; // uid为null时，每条结果所包含的最大行数，int

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private TagInfoRepository tagInfoRepository;

    @Override
    public List<LogPack> cut(QueryContext context, List<LogLine> lines) {
        // 对日志行进行分类
        Map<String, List<LogLine>> sortedUidMap = new LinkedHashMap<>();
        List<LogLine> nullUidList = new LinkedList<>();
        sortLogLines(lines, sortedUidMap, nullUidList);
        // 汇总并返回全部结果
        List<LogPack> results = new LinkedList<>();
        results.addAll(uidMapToPacks(sortedUidMap));
        results.addAll(nullUidListToPacks(context, nullUidList));
        return results;
    }

    // ********************内部方法********************

    private List<LogPack> uidMapToPacks(Map<String, List<LogLine>> sortedUidMap) {
        List<LogPack> results = new LinkedList<>();
        for (Map.Entry<String, List<LogLine>> entry : sortedUidMap.entrySet()) {
            String uid = entry.getKey();
            // 按线程拆分列表
            Map<String, List<LogLine>> splatMap = splitByThread(entry.getValue());
            for (Map.Entry<String, List<LogLine>> listEntry : splatMap.entrySet()) {
                String thread = listEntry.getKey();
                List<TagInfo> tagInfoList = tagInfoRepository.findByUidAndThreadOrderByTime(uid, thread);
                results.add(toPack(uid, thread, tagInfoList, listEntry.getValue()));
            }
        }
        return results;
    }

    private List<LogPack> nullUidListToPacks(QueryContext context, List<LogLine> lines) {
        // 按线程拆分列表
        List<LogPack> results = new LinkedList<>();
        Map<String, List<LogLine>> splatMap = splitByThread(lines);
        for (Map.Entry<String, List<LogLine>> entry : splatMap.entrySet()) {
            // 按数目限制再次拆分
            String maxLineCountString = context.getParam(PREFIX, P_KEY_MAX_LINES_PER_RESULT_WITH_NULL_UID);
            int maxLineCount = (null != maxLineCountString ? Integer.parseInt(maxLineCountString) : appConfig.maxLinesPerResultWithNullUid);
            Collection<List<LogLine>> splatLines = splitByLineCount(entry.getValue(), maxLineCount);
            // 转换为结果列表
            for (List<LogLine> lineList : splatLines) {
                results.add(toPack(null, entry.getKey(), null, lineList));
            }
        }
        return results;
    }

    private Map<String, List<LogLine>> splitByThread(List<LogLine> lines) {
        Map<String, List<LogLine>> groups = new HashMap<>();
        for (LogLine line : lines) {
            groups.computeIfAbsent(line.thread, k -> new LinkedList<>()).add(line);
        }
        return groups;
    }

    private Collection<List<LogLine>> splitByLineCount(List<LogLine> lines, int maxLineCount) {
        Collection<List<LogLine>> results = new LinkedList<>();
        for (int i = 0; i < lines.size(); i += maxLineCount) {
            int endIndex = Math.min(i + maxLineCount, lines.size());
            results.add(lines.subList(i, endIndex));
        }
        return results;
    }

    private void sortLogLines(List<LogLine> list, Map<String, List<LogLine>> sortedUidMap, List<LogLine> nullUidList) {
        for (LogLine logLine : list) {
            if (null == logLine.uid) {
                nullUidList.add(logLine);
            } else {
                sortedUidMap.computeIfAbsent(logLine.uid, k -> new LinkedList<>()).add(logLine);
            }
        }
    }

    private LogPack toPack(@Nullable String uid, String thread, @Nullable List<TagInfo> tags, @NonNull List<LogLine> lines) {
        LogPack result = new LogPack();
        result.uid = uid;
        result.thread = thread;
        result.tags = tags;
        result.logLines.addAll(lines);
        return result;
    }

}
