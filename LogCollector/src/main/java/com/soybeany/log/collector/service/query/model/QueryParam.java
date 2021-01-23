package com.soybeany.log.collector.service.query.model;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.core.model.LogException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class QueryParam {

    private static final String SEPARATOR = "-";
    private static final String P_KEY_FROM_TIME = "fromTime";
    private static final String P_KEY_TO_TIME = "toTime";
    private static final String P_KEY_COUNT_LIMIT = "countLimit";

    private static final Map<Integer, DateTimeFormatter> FORMATTER_MAP = new HashMap<>();

    private final Map<String, Map<String, String>> params = new HashMap<>();
    private final AppConfig appConfig;

    private String fromTime;
    private String toTime;
    private Integer countLimit;

    static {
        initFormatterMap();
    }

    public static String getParam(String prefix, String key, Map<String, String> param) {
        return param.get(prefix + SEPARATOR + key);
    }

    private static void initFormatterMap() {
        FORMATTER_MAP.put(19, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        FORMATTER_MAP.put(16, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        FORMATTER_MAP.put(17, DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss"));
        FORMATTER_MAP.put(14, DateTimeFormatter.ofPattern("yy-MM-dd HH:mm"));
    }

    public QueryParam(AppConfig appConfig, Map<String, String> param) {
        this.appConfig = appConfig;
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
        if (null == countLimit) {
            countLimit = appConfig.defaultMaxResultCount;
        }
    }

    // ********************公开方法********************

    public String getFromTime() {
        return fromTime;
    }

    public String getToTime() {
        return toTime;
    }

    public int getCountLimit() {
        return countLimit;
    }

    @NonNull
    public Map<String, String> getParams(String prefix) {
        return Optional.ofNullable(params.get(prefix)).orElseGet(HashMap::new);
    }

    @Nullable
    public String getParam(String prefix, String key) {
        return getParams(prefix).get(key);
    }

    // ********************内部方法********************

    private void handleFixKey(String key, String value) {
        switch (key) {
            case P_KEY_FROM_TIME:
                fromTime = parseTime(value);
                break;
            case P_KEY_TO_TIME:
                toTime = parseTime(value);
                break;
            case P_KEY_COUNT_LIMIT:
                countLimit = Integer.parseInt(value);
            default:
        }
    }

    private void postHandleTime() {
        // 确保时间参数不为null
        if (null == fromTime) {
            fromTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).minusSeconds(1).format(appConfig.lineTimeFormatter);
        }
        if (null == toTime) {
            toTime = LocalDateTime.now().format(appConfig.lineTimeFormatter);
        }
        // 校验时间参数
        LocalDateTime fTime = LocalDateTime.parse(fromTime, appConfig.lineTimeFormatter);
        LocalDateTime tTime = LocalDateTime.parse(toTime, appConfig.lineTimeFormatter);
        if (fTime.isAfter(tTime)) {
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

    private String parseTime(String string) {
        DateTimeFormatter formatter = FORMATTER_MAP.get(string.length());
        if (null == formatter) {
            throw new LogException("使用了不支持的时间格式");
        }
        return LocalDateTime.parse(string, formatter).format(appConfig.lineTimeFormatter);
    }

}
