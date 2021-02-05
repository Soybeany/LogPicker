package com.soybeany.log.collector.service.query.data;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.core.model.LogException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern LOG_FILE_TIME_PATTERN = Pattern.compile("<\\?(.+)\\?>");

    private static final Map<Integer, DateTimeParser> FORMATTER_MAP = new HashMap<>();

    private final Map<String, Map<String, String>> params = new HashMap<>();
    private final AppConfig appConfig;

    private String fromTime;
    private String toTime;
    private Integer countLimit;
    private final Set<File> logFiles = new LinkedHashSet<>();
    private final Set<String> uidSet = new LinkedHashSet<>();

    static {
        initFormatterMap();
    }

    public static String getParam(String prefix, String key, Map<String, String> param) {
        return param.get(prefix + SEPARATOR + key);
    }

    private static void initFormatterMap() {
        FORMATTER_MAP.put(19, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        FORMATTER_MAP.put(16, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        FORMATTER_MAP.put(17, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss")));
        FORMATTER_MAP.put(14, time -> LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yy-MM-dd HH:mm")));
        FORMATTER_MAP.put(8, time -> LocalDateTime.of(LocalDate.now(), LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm:ss"))));
        FORMATTER_MAP.put(5, time -> LocalDateTime.of(LocalDate.now(), LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))));
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
        postHandleCountLimit();
        postHandleLogFiles();
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

    public Set<File> getLogFiles() {
        return logFiles;
    }

    public Set<String> getUidSet() {
        return uidSet;
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
            fromTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).format(appConfig.lineTimeFormatter);
        }
        if (null == toTime) {
            toTime = LocalDateTime.now().format(appConfig.lineTimeFormatter);
        }
        // 校验时间参数
        LocalDateTime fTime = parseToDateTime(fromTime);
        LocalDateTime tTime = parseToDateTime(toTime);
        if (fTime.isAfter(tTime)) {
            throw new LogException("开始时间不能晚于结束时间");
        }
    }

    private void postHandleCountLimit() {
        if (null == countLimit) {
            countLimit = appConfig.defaultMaxResultCount;
        }
    }

    private void postHandleLogFiles() {
        // 若已指定日志文件，则不需再添加
        if (!logFiles.isEmpty()) {
            return;
        }
        // 补充日志文件
        LocalDate fDate = parseToDateTime(fromTime).toLocalDate();
        LocalDate tDate = parseToDateTime(toTime).toLocalDate();
        LocalDate today = LocalDate.now();
        LocalDate tempDate = fDate;
        while (!tempDate.isAfter(tDate)) {
            addFiles(today, tempDate);
            tempDate = tempDate.plusDays(1);
        }
    }

    private LocalDateTime parseToDateTime(String string) {
        return LocalDateTime.parse(string, appConfig.lineTimeFormatter);
    }

    private void addFiles(LocalDate today, LocalDate date) {
        if (today.isEqual(date)) {
            addFiles(appConfig.logTodayFileName);
        } else {
            addFiles(toFileName(appConfig.logHistoryFileName, date));
        }
    }

    private void addFiles(String fileName) {
        for (String dir : appConfig.dirsToScan) {
            File file = new File(dir, fileName);
            if (file.exists()) {
                logFiles.add(file);
            }
        }
    }

    private String toFileName(String template, LocalDate date) {
        Matcher matcher = LOG_FILE_TIME_PATTERN.matcher(template);
        if (!matcher.find()) {
            return template;
        }
        String timeString = date.format(DateTimeFormatter.ofPattern(matcher.group(1)));
        return matcher.replaceAll(timeString);
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
        DateTimeParser parser = FORMATTER_MAP.get(string.length());
        if (null == parser) {
            throw new LogException("使用了不支持的时间格式");
        }
        try {
            return parser.parse(string).format(appConfig.lineTimeFormatter);
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
