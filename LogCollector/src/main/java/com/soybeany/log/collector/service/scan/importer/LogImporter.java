package com.soybeany.log.collector.service.scan.importer;

import java.io.File;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public interface LogImporter {

    void onStart(int fileId, File file);

    void onRead(long startPointer, long endPointer, String line);

    void onFinish();

}
