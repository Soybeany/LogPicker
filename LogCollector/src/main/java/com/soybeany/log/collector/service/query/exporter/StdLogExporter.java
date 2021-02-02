package com.soybeany.log.collector.service.query.exporter;

import com.google.gson.Gson;
import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.collector.service.query.data.QueryResult;
import com.soybeany.log.core.model.*;
import com.soybeany.log.core.util.TimeUtils;
import com.soybeany.util.HexUtils;
import com.soybeany.util.SerializeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
@Component
class StdLogExporter implements LogExporter {

    private static final String P_KEY_EXPORT_TYPE = "exportType";

    private static final DateTimeFormatter FORMATTER1 = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER2 = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private AppConfig appConfig;

    private final String ipAddress = getIpAddress();
    private final Gson gson = new Gson();

    @Override
    public String export(QueryResult result, List<LogPack> packs) {
        QueryContext context = result.context;
        String exportType = Optional.ofNullable(context.getParam(PREFIX, P_KEY_EXPORT_TYPE)).orElse(Constants.EXPORT_FOR_DIRECT_READ);
        switch (exportType) {
            case Constants.EXPORT_FOR_DIRECT_READ:
                return gson.toJson(toObjectForRead(context, toLogVO(result, packs)));
            case Constants.EXPORT_FOR_READ:
                return gson.toJson(toLogVO(result, packs));
            case Constants.EXPORT_IN_SERIALIZE:
                QueryResultVO vo = getNewResultVO(result);
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

    private Object toObjectForRead(QueryContext context, QueryResultVO vo) {
        List<Object> output = new LinkedList<>();
        // 添加结果信息
        output.add(vo.info);
        // 添加额外信息
        Map<String, String> exInfo = new LinkedHashMap<>();
        exInfo.put("expectCount", context.queryParam.getCountLimit() + "");
        exInfo.put("actualCount", vo.packs.size() + "");
        output.add(exInfo);
        // 添加结果列表
        output.addAll(vo.packs);
        return output;
    }

    private QueryResultVO toLogVO(QueryResult result, List<LogPack> packs) {
        QueryResultVO vo = getNewResultVO(result);
        // 添加结果列表
        for (LogPack pack : packs) {
            vo.packs.add(toLogItem(pack));
        }
        // 排序
        vo.packs.sort(Comparator.comparing(o -> ((LogPackForRead) o).time));
        return vo;
    }

    private QueryResultVO getNewResultVO(QueryResult result) {
        QueryResultVO vo = new QueryResultVO();
        vo.info.lastContextId = result.lastId;
        vo.info.curContextId = result.id;
        vo.info.nextContextId = result.nextId;
        vo.info.msg = result.context.msgMap.toString();
        vo.info.endReason = result.endReason;
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
        result.server = ipAddress;
        result.uid = pack.uid;
        result.thread = pack.thread;
        return result;
    }

    private List<String> getLogs(LocalDateTime earliestTime, LogPack logPack) {
        List<String> logs = new LinkedList<>();
        for (LogLine line : logPack.logLines) {
            LocalDateTime dateTime = LocalDateTime.parse(line.time, appConfig.lineTimeFormatter);
            long deltaDays = dateTime.toLocalDate().toEpochDay() - earliestTime.toLocalDate().toEpochDay();
            String time = dateTime.format(FORMATTER2) + (0 != deltaDays ? "(+" + deltaDays + ")" : "");
            String log = time + " " + line.level + " " + line.content;
            logs.add(log);
        }
        return logs;
    }

    @NonNull
    private Map<String, String> getTags(LogPack logPack) {
        Map<String, String> result = new LinkedHashMap<>();
        if (logPack.tags.isEmpty()) {
            return result;
        }
        Map<String, List<String>> temp = new HashMap<>();
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

    @NonNull
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
        return TimeUtils.toDate(LocalDateTime.parse(timeString, appConfig.lineTimeFormatter));
    }

    private String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "未知";
        }
    }

    // ********************内部类********************

    private static class TimeInfo {
        Date tagStartTime;
        Date tagEndTime;
        Date firstTagTime;
        Date firstLogTime;
    }

}
