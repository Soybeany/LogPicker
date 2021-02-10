package com.soybeany.log.collector.query.exporter;

import com.google.gson.Gson;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.query.data.QueryContext;
import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.core.model.*;
import com.soybeany.log.core.util.TimeUtils;
import com.soybeany.util.HexUtils;
import com.soybeany.util.SerializeUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public class StdLogExporter implements LogExporter {

    private static final String P_KEY_EXPORT_TYPE = "exportType";
    private static final DateTimeFormatter FORMATTER1 = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER2 = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LogCollectConfig logCollectConfig;
    private final Gson gson = new Gson();

    public StdLogExporter(LogCollectConfig logCollectConfig) {
        this.logCollectConfig = logCollectConfig;
    }

    @Override
    public String export(QueryResult result, List<LogPack> packs) {
        QueryContext context = result.context;
        String exportType = Optional.ofNullable(context.getParam(PREFIX, P_KEY_EXPORT_TYPE)).orElse(Constants.EXPORT_FOR_DIRECT_READ);
        switch (exportType) {
            case Constants.EXPORT_FOR_DIRECT_READ:
                return gson.toJson(toObjectForRead(toLogVO(result, packs)));
            case Constants.EXPORT_FOR_PACK:
                return gson.toJson(toLogVO(result, packs));
            case Constants.EXPORT_ROF_RAW:
                QueryRawResultVO vo = new QueryRawResultVO();
                setupResultInfo(result, vo.info);
                vo.packs.addAll(packs);
                try {
                    return HexUtils.bytesToHex(SerializeUtils.serialize(vo));
                } catch (IOException e) {
                    throw new LogException("结果导出异常:" + e.getMessage());
                }
            default:
                throw new LogException("使用了不支持的导出类型");
        }
    }

    // ********************内部方法********************

    private Object toObjectForRead(QueryResultVO vo) {
        List<Object> output = new LinkedList<>();
        // 添加结果信息
        output.add(vo.info);
        // 添加结果列表
        output.addAll(vo.packs);
        return output;
    }

    private QueryResultVO toLogVO(QueryResult result, List<LogPack> packs) {
        QueryResultVO vo = new QueryResultVO();
        setupResultInfo(result, vo.info);
        // 添加结果列表
        for (LogPack pack : packs) {
            vo.packs.add(toLogItem(pack));
        }
        // 排序
        vo.packs.sort(Comparator.comparing(o -> o.time));
        return vo;
    }

    private void setupResultInfo(QueryResult result, ResultInfo info) {
        info.lastResultId = result.lastId;
        info.curResultId = result.id;
        info.nextResultId = result.nextId;
        info.msg = result.getAllMsg();
        info.endReason = result.endReason;
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
            LocalDateTime dateTime = LocalDateTime.parse(line.time, logCollectConfig.lineTimeFormatter);
            long deltaDays = dateTime.toLocalDate().toEpochDay() - earliestTime.toLocalDate().toEpochDay();
            String time = dateTime.format(FORMATTER2) + (0 != deltaDays ? "(+" + deltaDays + ")" : "");
            String log = time + " " + line.level + " " + line.content;
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
        if (null != info.tagStartTime) {
            return info.tagStartTime;
        }
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
        }
        if (null != result.endTag) {
            info.tagEndTime = getDate(result.endTag.time);
        }
        if (!result.tags.isEmpty()) {
            info.firstTagTime = getDate(result.tags.get(0).time);
        }
        if (!result.logLines.isEmpty()) {
            info.firstLogTime = getDate(result.logLines.get(0).time);
        }
        return info;
    }

    private Date getDate(String timeString) {
        return TimeUtils.toDate(LocalDateTime.parse(timeString, logCollectConfig.lineTimeFormatter));
    }

    // ********************内部类********************

    private static class TimeInfo {
        Date tagStartTime;
        Date tagEndTime;
        Date firstTagTime;
        Date firstLogTime;
    }

}
