package com.soybeany.log.collector.query.exporter;

import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.core.model.LogPack;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/10
 */
public interface LogExporter {

    String PREFIX = "exporter";

    String export(QueryResult result, List<LogPack> packs);

}