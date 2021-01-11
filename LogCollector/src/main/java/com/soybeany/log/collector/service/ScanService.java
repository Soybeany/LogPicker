package com.soybeany.log.collector.service;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.model.IQueryListener;
import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.core.model.LogException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface ScanService {
    // todo 全扫描，扫描指定目录，清理已经没有文件的数据
    // todo 精准扫描，只扫描指定的文件，查询时可以加参
    // todo 加锁后扫描
    // todo 扫描前，使用观察者模式，先禁止新查询，待全部已有查询结束后，再进行扫描

    void fullScan();

}

@Service
class ScanServiceImpl implements ScanService, IQueryListener {

    private static final String PREFIX = "scan";

    private static final String P_KEY_BEFORE_QUERY = "beforeQuery"; // 查询前进行扫描的文件列表，可使用today指代当天，string

    @Autowired
    private AppConfig appConfig;

    @Override
    public void fullScan() {
        File[] logs = new File(appConfig.dirToScan).listFiles();
        if (null == logs) {
            throw new LogException("指定的日志目录不存在");
        }

    }

    @Override
    public void onQuery(QueryContext context) {
        // todo 如果包含需先扫描的文件，则先执行扫描
    }
}