package com.soybeany.log.collector.query.exporter;

import com.google.gson.Gson;
import com.soybeany.log.core.model.QueryResultVO;

/**
 * @author Soybeany
 * @date 2021/3/23
 */
public abstract class GsonLogExporter extends BaseLogExporter<String> {

    private final Gson gson = new Gson();

    @Override
    protected String output(QueryResultVO vo) {
        return toString(gson, vo);
    }

    protected abstract String toString(Gson gson, QueryResultVO vo);

}
