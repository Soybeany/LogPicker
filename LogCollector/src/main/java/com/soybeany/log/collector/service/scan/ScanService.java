package com.soybeany.log.collector.service.scan;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.common.LogIndexService;
import com.soybeany.log.collector.service.common.model.LogIndexes;
import com.soybeany.log.collector.service.query.model.IQueryListener;
import com.soybeany.log.collector.service.query.model.QueryContext;
import com.soybeany.log.collector.service.scan.importer.IndexesImporter;
import com.soybeany.log.core.model.LogException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;

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
class ScanServiceImpl implements ScanService, IQueryListener {

    private static final String PREFIX = "scan";

    private static final String P_KEY_BEFORE_QUERY = "beforeQuery"; // 查询前进行扫描的文件列表，可使用today指代当天，string

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private LogIndexService logIndexService;
    @Autowired
    private IndexesImporter indexesImporter;

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
    public void onQuery(QueryContext context) {
        // todo 如果包含需先扫描的文件，则先执行扫描
    }

    // ********************内部方法********************

    private void scanFile(File file) {
        try {
            LogIndexes indexes = Optional.ofNullable(logIndexService.loadIndexes(file)).orElseGet(LogIndexes::new);
            indexesImporter.executeImport(indexes);
            logIndexService.saveIndexes(file, indexes);
        } catch (Exception e) {
            throw new LogException("日志文件扫描异常:" + e.getMessage());
        }
    }
}