package com.soybeany.log.writer;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Soybeany
 * @date 2021/2/18
 */
public abstract class TagWriteListener implements ServletRequestListener {

    private final String traceIdAttributeKey = onGetTraceIdAttributeKey();

    private final TagWriter tagWriter = onGetTagWriter();

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        ServletRequest r = sre.getServletRequest();
        // 非http请求不处理
        if (!(r instanceof HttpServletRequest)) {
            return;
        }
        HttpServletRequest request = (HttpServletRequest) r;
        if (!shouldWriteFlags(request)) {
            return;
        }
        // 设置并输出开始标签
        String traceId = onSetupTraceId(request);
        if (null != traceId) {
            TagWriter.setupTraceId(true, traceId);
        }
        tagWriter.writeStdBorderTag(onGetUrlToWrite(request), true);
        request.setAttribute(traceIdAttributeKey, TagWriter.getTraceId());
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        ServletRequest r = sre.getServletRequest();
        // 非http请求不处理
        if (!(r instanceof HttpServletRequest)) {
            return;
        }
        // 没有traceId的请求不处理
        HttpServletRequest request = (HttpServletRequest) r;
        if (hasNoStartFlag(request)) {
            return;
        }
        // 输出结束标签
        tagWriter.writeEndTag();
        TagWriter.removeTraceId();
    }

    // ********************子类重写********************

    protected String onGetTraceIdAttributeKey() {
        return "traceId";
    }

    protected TagWriter onGetTagWriter() {
        return new TagWriter();
    }

    protected String onGetUrlToWrite(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    protected String onSetupTraceId(HttpServletRequest request) {
        return null;
    }

    protected boolean shouldWriteFlags(HttpServletRequest request) {
        return true;
    }

    // ********************内部方法********************

    private boolean hasNoStartFlag(ServletRequest request) {
        return null == request.getAttribute(traceIdAttributeKey);
    }

}
