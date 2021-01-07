package com.soybeany.log.collector.service.selector;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.repository.LogLineInfo;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public interface LogSelector extends Comparable<LogSelector> {

    @Override
    default int compareTo(LogSelector o) {
        return o.priority() - priority();
    }

    /**
     * 优先级，值越大，越先被执行，默认值为0
     */
    default int priority() {
        return 0;
    }

    /**
     * 判断当前选择器是否支持此查询
     */
    boolean isSupport(QueryContext context);

    /**
     * 选择出合适的日志行
     *
     * @return 返回null或空列表，表示没有更多记录
     */
    @Nullable
    List<LogLineInfo> select(QueryContext context, int page, int pageSize);

}
