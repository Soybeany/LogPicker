package com.soybeany.log.collector.repository;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * 文件信息
 *
 * @author Soybeany
 * @date 2021/1/4
 */
@Entity
@Table
public class FileInfo extends BaseEntity {

    /**
     * 文件所在目录
     */
    @Column(nullable = false)
    public String dir;

    /**
     * 文件名
     */
    @Column(nullable = false)
    public String fileName;

    /**
     * 已扫描的字节数
     */
    @Column(nullable = false)
    public long scannedBytes;

    /**
     * 文件的最后修改时间
     */
    @Column(nullable = false)
    public Date lastModifyTime;

}
