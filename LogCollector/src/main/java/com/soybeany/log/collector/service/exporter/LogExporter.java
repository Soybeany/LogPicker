package com.soybeany.log.collector.service.exporter;

import com.soybeany.log.collector.model.LogPack;
import com.soybeany.log.collector.model.QueryContext;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/10
 */
public interface LogExporter {

    Object export(QueryContext context, List<LogPack> packs);

}
