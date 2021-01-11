package com.soybeany.log.collector.service.exporter;

import com.soybeany.log.collector.model.LogLine;
import com.soybeany.log.collector.model.LogPack;
import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.repository.TagInfo;
import com.soybeany.log.core.model.StdLogItem;
import com.soybeany.log.core.model.StdLogVO;
import com.soybeany.log.core.model.TagDesc;
import com.soybeany.log.core.util.TimeUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

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

    private static final String P_KEY_IS_FOR_READ = "isForRead";

    private static final DateTimeFormatter FORMATTER1 = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER2 = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String ipAddress = getIpAddress();

    @Override
    public Object export(QueryContext context, List<LogPack> packs) {
        // 转换为VO
        StdLogVO vo = toLogVO(context, packs);
        // 若为一般模式直接返回
        String isForRead = context.getParam(PREFIX, P_KEY_IS_FOR_READ);
        if (!Boolean.parseBoolean(isForRead)) {
            return vo;
        }
        // 若为阅读模式，特殊转换
        return toObjectForRead(vo);
    }

    // ********************内部方法********************

    private Object toObjectForRead(StdLogVO vo) {
        List<Object> result = new LinkedList<>();
        // 添加结果信息
        result.add(vo.info);
        // 添加结果列表
        result.addAll(vo.packs);
        return result;
    }

    private StdLogVO toLogVO(QueryContext context, List<LogPack> packs) {
        StdLogVO vo = new StdLogVO();
        // 设置结果信息
        StdLogVO.Info info = new StdLogVO.Info();
        info.lastContextId = context.lastId;
        info.curContextId = context.id;
        info.nextContextId = context.nextId;
        info.endReason = context.endReason;
        vo.info = info;
        // 添加结果列表
        for (LogPack pack : packs) {
            vo.packs.add(toLogItem(pack));
        }
        return vo;
    }

    private StdLogItem toLogItem(LogPack pack) {
        StdLogItem result = new StdLogItem();
        TimeInfo info = getTimeInfo(pack);
        // 设置时间
        LocalDateTime earliestTime = TimeUtils.toLocalDateTime(getEarliestTime(info));
        result.time = earliestTime.format(FORMATTER1);
        // 设置耗时
        result.spend = getSpend(info);
        // 设置标签集
        result.tags.putAll(getTags(pack));
        // 设置日志
        result.logs.addAll(getLogs(pack));
        // 设置其它简易信息
        result.server = ipAddress;
        result.uid = pack.uid;
        result.thread = pack.thread;
        return result;
    }

    private List<String> getLogs(LogPack raw) {
        // todo 拼装时间时，在日期右侧显示+n代表跨天，如:00:02(+1)
        List<String> logs = new LinkedList<>();
        for (LogLine line : raw.logLines) {
            String time = TimeUtils.toLocalDateTime(line.time).format(FORMATTER2);
            String log = time + " " + line.level + " " + line.content;
            logs.add(log);
        }
        return logs;
    }

    @NonNull
    private Map<String, String> getTags(LogPack raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (null != raw.tags) {
            Map<String, List<String>> temp = new HashMap<>();
            for (TagInfo info : raw.tags) {
                temp.computeIfAbsent(info.key, k -> new ArrayList<>()).add(info.value);
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
        }
        return result;
    }

    private String getSpend(TimeInfo timeInfo) {
        if (null == timeInfo.tagStartTime || null == timeInfo.tagEndTime) {
            return "缺失标签数据";
        }
        int deltaSec = (int) ((timeInfo.tagEndTime.getTime() - timeInfo.tagStartTime.getTime()) / 1000);
        return deltaSec + "s";
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
        if (null != result.tags && !result.tags.isEmpty()) {
            info.firstTagTime = result.tags.get(0).time;
            for (TagInfo tag : result.tags) {
                if (TagDesc.BORDER_START.equalsIgnoreCase(tag.key)) {
                    info.tagStartTime = tag.time;
                } else if ((TagDesc.BORDER_END.equalsIgnoreCase(tag.key))) {
                    info.tagEndTime = tag.time;
                }
            }
        }
        if (!result.logLines.isEmpty()) {
            info.firstLogTime = result.logLines.get(0).time;
        }
        return info;
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
