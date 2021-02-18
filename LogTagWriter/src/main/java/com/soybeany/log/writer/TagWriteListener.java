package com.soybeany.log.writer;

import org.slf4j.MDC;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Soybeany
 * @date 2021/2/18
 */
public abstract class TagWriteListener implements ServletRequestListener {

    private static final String KEY_TRACE_ID = "traceId";

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        ServletRequest r = sre.getServletRequest();
        // 非http请求不处理
        if (!(r instanceof HttpServletRequest)) {
            return;
        }
        HttpServletRequest request = (HttpServletRequest) r;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!shouldWriteFlags(request, path)) {
            return;
        }
        // 设置并输出开始标签
        TagWriter.setupTraceId(true);
        TagWriter.writeStartFlag();
        TagWriter.writeUrlFlag(path);
        request.setAttribute(KEY_TRACE_ID, MDC.get(KEY_TRACE_ID));
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
        TagWriter.writeEndFlag();
        TagWriter.removeTraceId();
    }

    // ********************子类重写********************

    protected boolean shouldWriteFlags(HttpServletRequest request, String path) {
        return true;
    }

    // ********************内部方法********************

    private boolean hasNoStartFlag(ServletRequest request) {
        return null == request.getAttribute(KEY_TRACE_ID);
    }

}
