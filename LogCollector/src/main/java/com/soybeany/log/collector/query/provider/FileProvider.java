package com.soybeany.log.collector.query.provider;

import com.soybeany.log.collector.query.data.FileParam;

import java.io.File;
import java.util.Set;

/**
 * @author Soybeany
 * @date 2021/3/23
 */
public interface FileProvider {

    /**
     * 根据配置、查询参数，得到待查询的文件集合
     */
    Set<File> onGetFiles(FileParam param);

}
