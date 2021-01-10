package com.soybeany.log.collector.service;

import com.soybeany.log.collector.config.AppConfig;
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
    // todo 精准扫描，只扫描指定的文件
    // todo 加锁后扫描
    // todo 扫描前，使用观察者模式，先禁止新查询，待全部已有查询结束后，再进行扫描

    void fullScan();

}

@Service
class ScanServiceImpl implements ScanService {

    @Autowired
    private AppConfig appConfig;

    @Override
    public void fullScan() {
        File[] logs = new File(appConfig.dirToScan).listFiles();
        if (null == logs) {
            throw new LogException("指定的日志目录不存在");
        }

    }
}