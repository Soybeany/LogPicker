package com.soybeany.log.collector.service.converter;

import com.soybeany.log.collector.model.LogLine;
import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.model.RawLogResult;
import com.soybeany.log.collector.repository.TagInfo;
import com.soybeany.log.core.model.LogResult;
import com.soybeany.log.core.model.TagDesc;
import com.soybeany.log.core.util.TimeUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/8
 */
public interface LogResultConvertService extends ConverterService<RawLogResult, LogResult> {
}

@Service
class LogResultConvertServiceImpl implements LogResultConvertService {

    private static final DateTimeFormatter FORMATTER1 = DateTimeFormatter.ofPattern("yy-MM-dd");
    private static final DateTimeFormatter FORMATTER2 = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String ipAddress = getIpAddress();

    @Override
    public List<LogResult> convert(QueryContext context, List<RawLogResult> list) {
        List<LogResult> results = new LinkedList<>();
        for (RawLogResult raw : list) {
            results.add(toResult(raw));
        }
        return results;
    }

    // ********************内部方法********************

    private LogResult toResult(RawLogResult raw) {
        LogResult result = new LogResult();
        TimeInfo info = getTimeInfo(raw);
        // 设置日期
        LocalDateTime earliestTime = TimeUtils.toLocalDateTime(getEarliestTime(info));
        result.date = earliestTime.format(FORMATTER1);
        // 设置耗时
        result.spend = getSpend(info);
        // 设置标签集
        result.tags.putAll(getTags(raw));
        // 设置日志
        result.logs.addAll(getLogs(raw));
        // 设置其它简易信息
        result.server = ipAddress;
        result.uid = raw.uid;
        result.thread = raw.thread;
        return result;
    }

    private List<String> getLogs(RawLogResult raw) {
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
    private Map<String, String> getTags(RawLogResult raw) {
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

    private TimeInfo getTimeInfo(RawLogResult result) {
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
