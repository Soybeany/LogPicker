package com.soybeany.log.collector.service.query.exporter;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.core.model.LogPack;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/10
 */
public interface LogExporter {

    String PREFIX = "exporter";

    String export(QueryContext context, List<LogPack> packs);

}
