package com.soybeany.log.collector.common.service;

import com.soybeany.log.collector.common.data.BaseUnit;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.model.loader.LogLineHolder;
import com.soybeany.log.core.model.LogPack;

import java.io.File;

/**
 * @author Soybeany
 * @date 2022/4/22
 */
public interface IUnitHandler<Unit extends BaseUnit> {

    Unit onGetNewUnit(LogCollectConfig logCollectConfig, File logFile);

    void onHandleLogLine(Unit unit, LogLineHolder holder);

    void onHandleLogPack(Unit unit, LogPack logPack);

}
