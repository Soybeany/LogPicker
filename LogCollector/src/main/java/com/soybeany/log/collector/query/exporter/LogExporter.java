package com.soybeany.log.collector.query.exporter;

import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.model.ResultInfo;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/10
 */
public interface LogExporter<T> {

    String PREFIX = "exporter";

    static void setupResultInfo(QueryResult result, ResultInfo info) {
        info.lastResultId = result.lastId;
        info.curResultId = result.id;
        info.nextResultId = result.nextId;
        info.msg = result.getAllMsg();
        info.endReason = result.endReason;
    }

    T export(QueryResult result, List<LogPack> packs);

}
