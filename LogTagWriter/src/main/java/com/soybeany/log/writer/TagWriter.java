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

    private static final Gson GSON = new Gson();
    private static final Logger LOG = LoggerFactory.getLogger(TagWriter.class);

    private static String KEY_TRACE_ID = "traceId";

    private String tagPrefix = "TAG";
    private String startTag = "border_start";
    private String endTag = "border_end";
    private String urlTag = "url";
    private String paramTag = "param";
    private String userTag = "user";
    private String infoTag = "info";

    // ********************TRACE_ID********************

    public static void setTraceIdKey(String key) {
        KEY_TRACE_ID = key;
    }

    public static void setupTraceId(boolean forceNew) {
        if (!forceNew && null != getTraceId()) {
            return;
        }
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(24);
        MDC.put(KEY_TRACE_ID, uuid);
    }

    public static String getTraceId() {
        return MDC.get(KEY_TRACE_ID);
    }

    public static void removeTraceId() {
        MDC.remove(KEY_TRACE_ID);
    }

    // ********************记录区********************

    public void writeStdBorderTag(String url, String info, boolean isStart) {
        if (isStart) {
            setupTraceId(false);
            writeStartTag();
            writeUrlTag(url);
            Optional.ofNullable(info).ifPresent(this::writeInfoTag);
        } else {
            writeEndTag();
            removeTraceId();
        }
    }

    public void writeStdRequestInfoTag(ServletRequest request, String user) {
        writeStdRequestInfoTag(request.getParameterMap(), user);
    }

    public void writeStdRequestInfoTag(Map<?, ?> paramMap, String user) {
        writeParamTag(paramMap);
        writeUserTag(user);
    }

    public void writeStdRequestInfoTag(String param, String user) {
        writeParamTag(param);
        writeUserTag(user);
    }

    public void writeStartTag() {
        writeTag(startTag, "");
    }

    public void writeEndTag() {
        writeTag(endTag, "");
    }

    public void writeUrlTag(String url) {
        writeTag(urlTag, url);
    }

    public void writeParamTag(Map<?, ?> paramMap) {
        if (!paramMap.isEmpty()) {
            writeParamTag(GSON.toJson(paramMap).replaceAll("\\\\", ""));
        }
    }

    public void writeParamTag(String param) {
        writeTag(paramTag, param);
    }

    public void writeUserTag(String user) {
        writeTag(userTag, user);
    }

    public void writeInfoTag(String info) {
        writeTag(infoTag, info);
    }

    /**
     * 写入标识日志<br/>
     * TAG格式：TAG-name(标签名)-content(标签内容)
     */
    public void writeTag(String key, String value) {
        LOG.info(tagPrefix + "-" + key + "-" + value);
    }

    // ********************内部类********************

    public static class Builder {
        private final TagWriter writer = new TagWriter();

        public Builder tagPrefix(String prefix) {
            writer.tagPrefix = prefix;
            return this;
        }

        public Builder startTag(String tag) {
            writer.startTag = tag;
            return this;
        }

        public Builder endTag(String tag) {
            writer.endTag = tag;
            return this;
        }

        public Builder urlTag(String tag) {
            writer.urlTag = tag;
            return this;
        }

        public Builder paramTag(String tag) {
            writer.paramTag = tag;
            return this;
        }

        public Builder userTag(String tag) {
            writer.userTag = tag;
            return this;
        }

        public Builder infoTag(String tag) {
            writer.infoTag = tag;
            return this;
        }

        public TagWriter get() {
            return writer;
        }
    }

}
