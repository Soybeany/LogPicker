package com.soybeany.log.collector.query.exporter;

import com.google.gson.Gson;
import com.soybeany.log.core.model.QueryResultVO;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/3/3
 */
public class DirectReadLogExporter extends GsonLogExporter {

    @Override
    protected String toString(Gson gson, QueryResultVO vo) {
        return gson.toJson(toObjectForRead(vo));
    }

    private Object toObjectForRead(QueryResultVO vo) {
        List<Object> output = new LinkedList<>();
        // 添加结果信息
        output.add(vo.info);
        // 添加结果列表
        output.addAll(vo.packs);
        return output;
    }
}
