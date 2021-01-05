package com.soybeany.log.collector.model;

import com.soybeany.log.collector.util.TimeUtils;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class QueryParam {

    private static final String SEPARATOR = "-";
    private static final String KEY_FROM_TIME = "fromTime";
    private static final String KEY_TO_TIME = "toTime";

    private static final Map<Integer, DateTimeFormatter> FORMATTER_MAP = new HashMap<>();

    private final Map<String, Map<String, String>> params = new HashMap<>();
    private LocalDateTime from;
    private LocalDateTime to;

    static {
        initFormatterMap();
    }

    private static void initFormatterMap() {
        FORMATTER_MAP.put(19, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        FORMATTER_MAP.put(16, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        FORMATTER_MAP.put(17, DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss"));
        FORMATTER_MAP.put(14, DateTimeFormatter.ofPattern("yy-MM-dd HH:mm"));
    }

    public QueryParam(Map<String, String> param) {
        // 处理入参
        for (Map.Entry<String, String> entry : param.entrySet()) {
            // 处理动态kv
            if (handleKv(entry)) {
                continue;
            }
            // 处理固定key
            handleFixKey(entry.getKey(), entry.getValue());
        }
        // 后处理参数
        postHandleTime();
    }

    // ********************公开方法********************

    public Date getFrom() {
        return TimeUtils.toDate(from);
    }

    public Date getTo() {
        return TimeUtils.toDate(to);
    }

    @NonNull
    public Map<String, String> getParams(String prefix) {
        return Optional.ofNullable(params.get(prefix)).orElseGet(HashMap::new);
    }

    // ********************内部方法********************

    private void handleFixKey(String key, String value) {
        switch (key) {
            case KEY_FROM_TIME:
                from = parseTime(value);
                break;
            case KEY_TO_TIME:
                to = parseTime(value);
                break;
            default:
        }
    }

    private void postHandleTime() {
        // 确保时间参数不为null
        if (null == from) {
            from = getDefaultFrom();
        }
        if (null == to) {
            to = getDefaultTo();
        }
        // 校验时间参数
        if (from.isAfter(to)) {
            throw new LogException("开始时间不能晚于结束时间");
        }
    }

    /**
     * @return 是否匹配
     */
    private boolean handleKv(Map.Entry<String, String> entry) {
        String[] parts = entry.getKey().split(SEPARATOR);
        if (parts.length < 2) {
            return false;
        }
        Map<String, String> map = params.computeIfAbsent(parts[0], k -> new LinkedHashMap<>());
        map.put(parts[1], entry.getValue());
        return true;
    }

    private LocalDateTime parseTime(String string) {
        DateTimeFormatter formatter = FORMATTER_MAP.get(string.length());
        if (null == formatter) {
            throw new LogException("使用了不支持的时间格式");
        }
        return LocalDateTime.parse(string, formatter);
    }

    private LocalDateTime getDefaultFrom() {
        return LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).minusSeconds(1);
    }

    private LocalDateTime getDefaultTo() {
        return LocalDateTime.now();
    }

    // ********************内部类********************

    public interface ParamHandler {
        boolean hasParams(QueryParam param);
    }

}
