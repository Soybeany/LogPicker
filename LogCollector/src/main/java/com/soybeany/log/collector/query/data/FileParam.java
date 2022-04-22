package com.soybeany.log.collector.query.data;

import com.soybeany.log.core.model.LogException;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2022/4/22
 */
public class FileParam {

    private static final Map<Integer, DateTimeParser> FORMATTER_MAP = new HashMap<>();

    private LocalDateTime fromTime;
    private LocalDateTime toTime;

    private final LinkedHashSet<File> logFiles = new LinkedHashSet<>();

    static {
        FORMATTER_MAP.put(19, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        FORMATTER_MAP.put(16, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        FORMATTER_MAP.put(17, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss")));
        FORMATTER_MAP.put(14, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yy-MM-dd HH:mm")));
        FORMATTER_MAP.put(8, time -> LocalDateTime.of(LocalDate.now(), LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm:ss"))));
        FORMATTER_MAP.put(5, time -> LocalDateTime.of(LocalDate.now(), LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))));
    }

    public void setFromTime(String value, boolean validate) {
        fromTime = parseTime(value);
        if (validate) {
            validateTime();
        }
    }

    public void setToTime(String value, boolean validate) {
        toTime = parseTime(value);
        if (validate) {
            validateTime();
        }
    }

    public void setLogFiles(String value) {
        for (String path : value.split("[;,]")) {
            File file = new File(path);
            if (!file.exists()) {
                throw new LogException("“" + path + "”文件不存在");
            }
            logFiles.add(file);
        }
    }

    /**
     * 补充/验证时间
     */
    public void validateTime() {
        // 确保时间参数不为null
        if (null == fromTime) {
            fromTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (null == toTime) {
            toTime = LocalDateTime.now();
        }
        // 校验时间参数
        if (fromTime.isAfter(toTime)) {
            throw new LogException("开始时间不能晚于结束时间");
        }
    }

    public LocalDateTime getFromTime() {
        return fromTime;
    }

    public LocalDateTime getToTime() {
        return toTime;
    }

    public LinkedHashSet<File> getLogFiles() {
        return logFiles;
    }

    // ***********************内部方法****************************

    private LocalDateTime parseTime(String string) {
        DateTimeParser parser = FORMATTER_MAP.get(string.length());
        if (null == parser) {
            throw new LogException("使用了不支持的时间格式");
        }
        try {
            return parser.parse(string);
        } catch (DateTimeParseException e) {
            throw new LogException("“" + string + "”时间解析异常");
        }
    }

    // ********************内部类********************

    private interface DateTimeParser {
        LocalDateTime parse(String string);
    }

}
