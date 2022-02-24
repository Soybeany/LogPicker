package com.soybeany.log.collector.query.exporter;

import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.core.model.*;
import com.soybeany.log.core.util.TimeUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/3/3
 */
public abstract class BaseLogExporter<T> implements LogExporter<T> {

    private static final DateTimeFormatter FORMATTER1 = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER2 = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ********************重写方法********************

    @Override
    public T export(QueryResult result) {
        return output(toLogVO(result));
    }

    // ********************子类方法********************

    protected void onSortLogs(List<LogPackForRead> packs) {
        packs.sort(Comparator.comparing(o -> o.time));
    }

    // ********************内部方法********************

    private QueryResultVO toLogVO(QueryResult result) {
        checkQueryResult(result);
        QueryResultVO vo = new QueryResultVO();
        LogExporter.setupResultInfo(result, vo.info);
        // 添加结果列表
        for (LogPack pack : result.logPacks) {
            vo.packs.add(toLogItem(pack));
        }
        // 排序
        onSortLogs(vo.packs);
        return vo;
    }

    private LogPackForRead toLogItem(LogPack pack) {
        LogPackForRead result = new LogPackForRead();
        TimeInfo info = getTimeInfo(pack);
        // 设置时间
        LocalDateTime earliestTime = TimeUtils.toLocalDateTime(getEarliestTime(info));
        result.time = earliestTime.format(FORMATTER1);
        // 设置耗时
        result.spend = getSpend(info);
        // 设置标签集
        result.tags.putAll(getTags(pack));
        // 设置日志
        result.logs.addAll(getLogs(earliestTime, pack));
        // 设置其它简易信息
        result.uid = pack.uid;
        result.thread = pack.thread;
        return result;
    }

    private List<String> getLogs(LocalDateTime earliestTime, LogPack logPack) {
        List<String> logs = new LinkedList<>();
        for (LogLine line : logPack.logLines) {
            long deltaDays = line.time.toLocalDate().toEpochDay() - earliestTime.toLocalDate().toEpochDay();
            String time = line.time.format(FORMATTER2) + (0 != deltaDays ? "(+" + deltaDays + ")" : "");
            String log = time + " " + line.level + " " + line.content;
            if (!line.pos.isEmpty()) {
                log += "(" + line.pos + ")";
            }
            logs.add(log);
        }
        return logs;
    }

    private Map<String, String> getTags(LogPack logPack) {
        Map<String, String> result = new LinkedHashMap<>();
        if (logPack.tags.isEmpty()) {
            return result;
        }
        Map<String, List<String>> temp = new LinkedHashMap<>();
        for (LogTag tag : logPack.tags) {
            temp.computeIfAbsent(tag.key, k -> new ArrayList<>()).add(tag.value);
        }
        for (Map.Entry<String, List<String>> entry : temp.entrySet()) {
            List<String> valueList = entry.getValue();
            if (valueList.size() == 1) {
                result.put(entry.getKey(), valueList.get(0));
            }
            // 为相同的tag重命名
            else {
                for (int i = 0; i < valueList.size(); i++) {
                    result.put(entry.getKey() + (i + 1), valueList.get(i));
                }
            }
        }
        return result;
    }

    private String getSpend(TimeInfo timeInfo) {
        if (null == timeInfo.tagStartTime || null == timeInfo.tagEndTime) {
            return "缺失标签数据";
        }
        int deltaSec = (int) ((timeInfo.tagEndTime.getTime() - timeInfo.tagStartTime.getTime()) / 1000);
        return deltaSec >= 1 ? deltaSec + "s" : "<1s";
    }

    private Date getEarliestTime(TimeInfo info) {
        if (null == info.firstTagTime) {
            return info.firstLogTime;
        }
        if (null == info.firstLogTime) {
            return info.firstTagTime;
        }
        return info.firstTagTime.before(info.firstLogTime) ? info.firstTagTime : info.firstLogTime;
    }

    private TimeInfo getTimeInfo(LogPack result) {
        TimeInfo info = new TimeInfo();
        if (null != result.startTag) {
            info.tagStartTime = getDate(result.startTag.time);
            setupFirstTagTime(info, info.tagStartTime);
        }
        if (null != result.endTag) {
            info.tagEndTime = getDate(result.endTag.time);
            setupFirstTagTime(info, info.tagEndTime);
        }
        if (!result.tags.isEmpty()) {
            setupFirstTagTime(info, getDate(result.tags.get(0).time));
        }
        if (!result.logLines.isEmpty()) {
            info.firstLogTime = getDate(result.logLines.get(0).time);
        }
        return info;
    }

    private void setupFirstTagTime(TimeInfo info, Date candidate) {
        if (null != info.firstTagTime && !candidate.before(info.firstTagTime)) {
            return;
        }
        info.firstTagTime = candidate;
    }

    private Date getDate(LocalDateTime time) {
        return TimeUtils.toDate(time);
    }

    // ********************抽象方法********************

    protected abstract T output(QueryResultVO vo);

    // ********************内部类********************

    private static class TimeInfo {
        Date tagStartTime;
        Date tagEndTime;
        Date firstTagTime;
        Date firstLogTime;
    }

}
