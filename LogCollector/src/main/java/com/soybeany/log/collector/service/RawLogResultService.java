package com.soybeany.log.collector.service;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.model.LogLine;
import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.RawLogResult;
import com.soybeany.log.collector.repository.LogLineInfo;
import com.soybeany.log.collector.repository.TagInfo;
import com.soybeany.log.collector.repository.TagInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/6
 */
public interface RawLogResultService {

    @NonNull
    List<RawLogResult> toRawLogResults(QueryContext context, List<LogLineInfo> list);

}

@Service
class RawLogResultServiceImpl implements RawLogResultService {

    private static final String PREFIX = "rawLogResult";

    private static final String P_KEY_MAX_LINES_PER_RESULT_WITH_NULL_UID = "maxLinesPerResultWithNullUid"; // uid为null时，每条结果所包含的最大行数，int

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private ModelConvertService modelConvertService;
    @Autowired
    private TagInfoRepository tagInfoRepository;

    @Override
    public List<RawLogResult> toRawLogResults(QueryContext context, List<LogLineInfo> list) {
        // 若没待处理的日志行，直接返回空列表
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        // 转换为行列表
        List<LogLine> lines = modelConvertService.toLineList(list);
        // 对日志行进行分类
        Map<String, List<LogLine>> sortedUidMap = new HashMap<>();
        List<LogLine> nullUidList = new LinkedList<>();
        sortLogLines(lines, sortedUidMap, nullUidList);
        // 汇总并返回全部结果
        List<RawLogResult> results = new LinkedList<>();
        results.addAll(uidMapToResults(sortedUidMap));
        results.addAll(nullUidListToResults(context, nullUidList));
        return results;
    }

    // ********************内部方法********************

    private List<RawLogResult> uidMapToResults(Map<String, List<LogLine>> sortedUidMap) {
        List<RawLogResult> results = new LinkedList<>();
        for (Map.Entry<String, List<LogLine>> entry : sortedUidMap.entrySet()) {
            List<TagInfo> tagInfoList = tagInfoRepository.findByUid(entry.getKey());
            // 按线程拆分列表
            Map<String, List<LogLine>> splatMap = splitByThread(entry.getValue());
            for (Map.Entry<String, List<LogLine>> listEntry : splatMap.entrySet()) {
                results.add(modelConvertService.toRawResult(entry.getKey(), listEntry.getKey(), tagInfoList, listEntry.getValue()));
            }
        }
        return results;
    }

    private List<RawLogResult> nullUidListToResults(QueryContext context, List<LogLine> lines) {
        // 按线程拆分列表
        List<RawLogResult> results = new LinkedList<>();
        Map<String, List<LogLine>> splatMap = splitByThread(lines);
        for (Map.Entry<String, List<LogLine>> entry : splatMap.entrySet()) {
            // 按数目限制再次拆分
            String maxLineCountString = context.queryParam.getParams(PREFIX).get(P_KEY_MAX_LINES_PER_RESULT_WITH_NULL_UID);
            int maxLineCount = (null != maxLineCountString ? Integer.parseInt(maxLineCountString) : appConfig.maxLinesPerResultWithNullUid);
            Collection<List<LogLine>> splatLines = splitByLineCount(entry.getValue(), maxLineCount);
            // 转换为结果列表
            for (List<LogLine> lineList : splatLines) {
                results.add(modelConvertService.toRawResult(null, entry.getKey(), null, lineList));
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

}
