package com.soybeany.log.collector.query.data;

import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.core.model.Constants;
import com.soybeany.log.core.model.LogException;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class QueryParam {

    private static final String SEPARATOR = "-";
    private static final String P_KEY_FROM_TIME = "fromTime";
    private static final String P_KEY_TO_TIME = "toTime";
    private static final String P_KEY_COUNT_LIMIT = "countLimit";
    private static final String P_KEY_LOG_FILES = "logFiles";
    private static final String P_KEY_UID_LIST = "uidList";

    private static final Map<Integer, DateTimeParser> FORMATTER_MAP = new HashMap<>();

    private final Map<String, Map<String, String[]>> params = new HashMap<>();
    private final LogCollectConfig logCollectConfig;

    private LocalDateTime fromTime;
    private LocalDateTime toTime;
    private Integer countLimit;
    private final LinkedHashSet<File> logFiles = new LinkedHashSet<>();
    private final LinkedHashSet<String> uidSet = new LinkedHashSet<>();

    static {
        initFormatterMap();
    }

    public static String getResultId(Map<String, String[]> param) {
        return getSingleValue(param.get(Constants.PARAM_RESULT_ID));
    }

    public static Map<String, String[]> toMultiValueMap(Map<String, String> param) {
        Map<String, String[]> result = new HashMap<>();
        param.forEach((k, v) -> result.put(k, new String[]{v}));
        return result;
    }

    private static String getSingleValue(String[] arr) {
        if (null == arr) {
            return null;
        }
        return 0 != arr.length ? arr[0] : null;
    }

    private static void initFormatterMap() {
        FORMATTER_MAP.put(19, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        FORMATTER_MAP.put(16, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        FORMATTER_MAP.put(17, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss")));
        FORMATTER_MAP.put(14, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yy-MM-dd HH:mm")));
        FORMATTER_MAP.put(8, time -> LocalDateTime.of(LocalDate.now(), LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm:ss"))));
        FORMATTER_MAP.put(5, time -> LocalDateTime.of(LocalDate.now(), LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))));
    }

    public QueryParam(LogCollectConfig logCollectConfig, Map<String, String[]> param) {
        this.logCollectConfig = logCollectConfig;
        // 处理入参
        for (Map.Entry<String, String[]> entry : param.entrySet()) {
            String[] value = entry.getValue();
            if (null == value || 0 == value.length) {
                continue;
            }
            // 处理动态kv
            if (handleKv(entry)) {
                continue;
            }
            // 处理固定key
            handleFixKey(entry.getKey(), value[0]);
        }
        // 后处理参数
        postHandleTime();
        postHandleCountLimit();
    }

    // ********************公开方法********************

    public LocalDateTime getFromTime() {
        return fromTime;
    }

    public LocalDateTime getToTime() {
        return toTime;
    }

    public int getCountLimit() {
        return countLimit;
    }

    public LinkedHashSet<File> getLogFiles() {
        return logFiles;
    }

    public LinkedHashSet<String> getUidSet() {
        return uidSet;
    }

    public Map<String, String[]> getParams(String prefix) {
        return Optional.ofNullable(params.get(prefix)).orElse(Collections.emptyMap());
    }

    public String[] getParam(String prefix, String key) {
        return getParams(prefix).get(key);
    }

    public String getSingleParam(String prefix, String key) {
        return getSingleValue(getParam(prefix, key));
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
                break;
            case P_KEY_LOG_FILES:
                parseAndAddLogFiles(value);
                break;
            case P_KEY_UID_LIST:
                parseAndAddUidList(value);
                break;
            default:
        }
    }

    private void postHandleTime() {
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

    private void postHandleCountLimit() {
        if (null == countLimit) {
            countLimit = logCollectConfig.defaultMaxResultCount;
        }
    }

    /**
     * @return 是否匹配
     */
    private boolean handleKv(Map.Entry<String, String[]> entry) {
        String[] parts = entry.getKey().split(SEPARATOR);
        if (parts.length < 2) {
            return false;
        }
        Map<String, String[]> map = params.computeIfAbsent(parts[0], k -> new LinkedHashMap<>());
        map.put(parts[1], entry.getValue());
        return true;
    }

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

    private void parseAndAddLogFiles(String string) {
        for (String path : string.split("[;,]")) {
            File file = new File(path);
            if (!file.exists()) {
                throw new LogException("“" + path + "”文件不存在");
            }
            logFiles.add(file);
        }
    }

    private void parseAndAddUidList(String string) {
        uidSet.addAll(Arrays.asList(string.split("[;,]")));
    }

    // ********************内部类********************

    private interface DateTimeParser {
        LocalDateTime parse(String string);
    }

}
