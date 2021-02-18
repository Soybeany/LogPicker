package com.soybeany.log.writer;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Soybeany
 * @date 2021/2/18
 */
public class TagWriter {

    private static final String FLAG_TAG = "FLAG";
    private static final String KEY_TRACE_ID = "traceId";

    private static final Gson GSON = new Gson();
    private static final Logger LOG = LoggerFactory.getLogger(TagWriter.class);

    // ********************记录区********************

    public static void setupTraceId(boolean forceNew) {
        if (!forceNew && null != MDC.get(KEY_TRACE_ID)) {
            return;
        }
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(24);
        MDC.put(KEY_TRACE_ID, uuid);
    }

    public static void removeTraceId() {
        MDC.remove(KEY_TRACE_ID);
    }

    public static void writeStdBorderFlag(String url, String info, boolean isStart) {
        if (isStart) {
            setupTraceId(false);
            writeStartFlag();
            writeUrlFlag(url);
            Optional.ofNullable(info).ifPresent(TagWriter::writeInfoFlag);
        } else {
            writeEndFlag();
            removeTraceId();
        }
    }

    public static void writeStdRequestInfoFlag(ServletRequest request, String user) {
        writeStdRequestInfoFlag(request.getParameterMap(), user);
    }

    public static void writeStdRequestInfoFlag(Map<?, ?> paramMap, String user) {
        writeParamFlag(paramMap);
        writeUserFlag(user);
    }

    public static void writeStdRequestInfoFlag(String param, String user) {
        writeParamFlag(param);
        writeUserFlag(user);
    }

    public static void writeStartFlag() {
        writeFlag("border_start", "");
    }

    public static void writeEndFlag() {
        writeFlag("border_end", "");
    }

    public static void writeUrlFlag(String url) {
        writeFlag("url", url);
    }

    public static void writeParamFlag(Map<?, ?> paramMap) {
        if (!paramMap.isEmpty()) {
            writeParamFlag(GSON.toJson(paramMap).replaceAll("\\\\", ""));
        }
    }

    public static void writeParamFlag(String param) {
        writeFlag("param", param);
    }

    public static void writeUserFlag(String user) {
        writeFlag("user", user);
    }

    public static void writeInfoFlag(String info) {
        writeFlag("info", info);
    }

    /**
     * 写入标识日志<br/>
     * FLAG格式：FLAG-name(标签名)-content(标签内容)
     */
    public static void writeFlag(String key, String value) {
        LOG.info(FLAG_TAG + "-" + key + "-" + value);
    }

}
