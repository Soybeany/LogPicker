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

    private static final String KEY_TRACE_ID = "traceId";

    private final TagWriter tagWriter = onGetTagWriter();

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
        tagWriter.writeStartTag();
        tagWriter.writeUrlTag(path);
        request.setAttribute(KEY_TRACE_ID, TagWriter.getTraceId());
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

    protected TagWriter onGetTagWriter() {
        return new TagWriter();
    }

    protected boolean shouldWriteFlags(HttpServletRequest request, String path) {
        return true;
    }

    // ********************内部方法********************

    private boolean hasNoStartFlag(ServletRequest request) {
        return null == request.getAttribute(KEY_TRACE_ID);
    }

}
