package com.soybeany.log.collector.service.scan;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.LogIndexService;
import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.core.model.LogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    // todo 每段末尾有换行符的问题，改为重新解析可解决

    /**
     * 执行全扫描
     */
    void fullScan();

    /**
     * 扫描指定的文件，多个文件使用“;”进行分隔
     */
    void scanFiles(String paths);

}

@Service
class ScanServiceImpl implements ScanService, LogIndexes.Updater {

    private static final Logger LOG = LoggerFactory.getLogger(Object.class);

    private static final String PREFIX = "scan";

    private static final String P_KEY_BEFORE_QUERY = "beforeQuery"; // 查询前进行扫描的文件列表，可使用today指代当天，string

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private LogIndexService logIndexService;

    @Override
    public void fullScan() {
        for (String dir : appConfig.dirsToScan) {
            File[] files = new File(dir).listFiles();
            if (null == files) {
                throw new LogException("指定的日志目录不存在");
            }
            for (File file : files) {
                scanFile(file);
            }
        }
    }

    @Override
    public void scanFiles(String paths) {

    }

    @Override
    public LogIndexes updateAndGet(File logFile) {
        return scanFile(logFile);
    }

    // ********************内部方法********************

    private LogIndexes scanFile(File file) {
        try {
            return logIndexService.updateAndGetIndexes(file);
        } catch (Exception e) {
            throw new LogException("日志文件扫描异常:" + e.getMessage());
        }
    }

}