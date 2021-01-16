package com.soybeany.log.collector.service.query.limiter;

import com.soybeany.log.collector.service.query.model.QueryContext;
import com.soybeany.log.core.model.LogPack;

/**
 * @author Soybeany
 * @date 2021/1/6
 */
public interface LogLimiter {

    String PREFIX = "limiter";

    /**
     * 此限制器的描述
     */
    String getDesc();

    /**
     * 是否能够添加日志结果
     */
    boolean canAddResult(QueryContext context, LogPack result);

}
