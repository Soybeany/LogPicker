package com.soybeany.log.collector.service.scan;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.model.IQueryListener;
import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.collector.repository.FileInfo;
import com.soybeany.log.collector.repository.FileInfoRepository;
import com.soybeany.log.collector.service.scan.importer.LogImporter;
import com.soybeany.log.core.model.LogException;
import com.soybeany.util.HexUtils;
import com.soybeany.util.file.BufferedRandomAccessFile;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface ScanService {
    // todo 全扫描，扫描指定目录，清理已经没有文件的数据
    // todo 精准扫描，只扫描指定的文件，查询时可以加参
    // todo 加锁后扫描
    // todo 扫描前，使用观察者模式，先禁止新查询，待全部已有查询结束后，再进行扫描
    // todo 分批次进行保存
    // todo sqlite大小写敏感，应在字段创建时使用 COLLATE NOCASE
    // todo 部分内容需要trim后再入库

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

    private static final String SEPARATOR = ";";
    private static final String PREFIX = "scan";

    private static final String P_KEY_BEFORE_QUERY = "beforeQuery"; // 查询前进行扫描的文件列表，可使用today指代当天，string

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private FileInfoRepository fileInfoRepository;
    @Autowired
    private ObjectFactory<LogImporter> logImporterFactory;

    @Override
    public void fullScan() {
        for (String dir : appConfig.dirsToScan.split(SEPARATOR)) {
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
            FileInfo info = toFileInfo(file);
            randomReadLine(info, file, logImporterFactory.getObject());
        } catch (Exception e) {
            throw new LogException("日志文件扫描异常:" + e.getMessage());
        }
    }

    private void randomReadLine(FileInfo info, File file, LogImporter importer) throws IOException {
        try (RandomAccessFile raf = new BufferedRandomAccessFile(file, "r");) {
            long pointer = info.scannedBytes;
            raf.seek(pointer);
            String rawLine;
            importer.onStart(info.getId(), file);
            while (null != (rawLine = raf.readLine())) {
                String line = new String(rawLine.getBytes(StandardCharsets.ISO_8859_1), "GBK");
                importer.onRead(pointer, pointer = raf.getFilePointer(), line);
            }
            importer.onFinish();
        }
    }

    private FileInfo toFileInfo(File file) throws NoSuchAlgorithmException {
        String pathMd5 = toPathMd5(file);
        FileInfo info = fileInfoRepository.findByPathMd5(pathMd5);
        if (null == info) {
            info = new FileInfo();
            info.dir = file.getParent();
            info.fileName = file.getName();
            info.pathMd5 = pathMd5;
            info.scannedBytes = 0L;
            info.lastModifyTime = new Date(file.lastModified());
            fileInfoRepository.save(info);
        }
        return info;
    }

    private String toPathMd5(File file) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(file.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
        return HexUtils.bytesToHex(md.digest());
    }

}