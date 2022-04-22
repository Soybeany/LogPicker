package com.soybeany.log.collector.query.data;

import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.core.model.Constants;

import java.time.LocalDateTime;
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

    private final Map<String, Map<String, String[]>> params = new HashMap<>();
    private final LogCollectConfig logCollectConfig;

    private Integer countLimit;
    private final FileParam fileParam = new FileParam();
    private final LinkedHashSet<String> uidSet = new LinkedHashSet<>();

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
        return fileParam.getFromTime();
    }

    public LocalDateTime getToTime() {
        return fileParam.getToTime();
    }

    public int getCountLimit() {
        return countLimit;
    }

    public FileParam getFileParam() {
        return fileParam;
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
                fileParam.setFromTime(value, false);
                break;
            case P_KEY_TO_TIME:
                fileParam.setToTime(value, false);
                break;
            case P_KEY_COUNT_LIMIT:
                countLimit = Integer.parseInt(value);
                break;
            case P_KEY_LOG_FILES:
                fileParam.setLogFiles(value);
                break;
            case P_KEY_UID_LIST:
                parseAndAddUidList(value);
                break;
            default:
        }
    }

    private void postHandleTime() {
        fileParam.validateTime();
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

    private void parseAndAddUidList(String string) {
        uidSet.addAll(Arrays.asList(string.split("[;,]")));
    }

}
