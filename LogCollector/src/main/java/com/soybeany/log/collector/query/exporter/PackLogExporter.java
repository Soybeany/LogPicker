package com.soybeany.log.collector.query.exporter;

import com.google.gson.Gson;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.core.model.QueryResultVO;

/**
 * @author Soybeany
 * @date 2021/3/3
 */
public class PackLogExporter extends BaseLogExporter {

    public PackLogExporter(LogCollectConfig logCollectConfig) {
        super(logCollectConfig);
    }

    @Override
    protected String toString(Gson gson, QueryResultVO vo) {
        return gson.toJson(vo);
    }
}
