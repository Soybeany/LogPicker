package com.soybeany.log.core.model;

import java.io.Serializable;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/6
 */
public class ResultVO implements Serializable {

    /**
     * 上一查询结果的上下文id
     */
    public final String lastContextId;

    /**
     * 当前查询结果的上下文id
     */
    public final String curContextId;

    /**
     * 下一查询结果的上下文id
     */
    public final String nextContextId;

    /**
     * 结束原因
     */
    public String endReason = "已搜索全部日志";

    /**
     * 匹配结果的列表
     */
    public final List<LogResult> logResults;

    public ResultVO(String lastContextId, String curContextId, String nextContextId, List<LogResult> logResults) {
        this.lastContextId = lastContextId;
        this.curContextId = curContextId;
        this.nextContextId = nextContextId;
        this.logResults = logResults;
    }
}
