package com.soybeany.log.collector.repository;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Date;

/**
 * 日志行信息
 *
 * @author Soybeany
 * @date 2021/1/4
 */
@Entity
@Table(indexes = {
        @Index(name = "uid_time_index", columnList = "uid"),
        @Index(name = "uid_time_index", columnList = "time"),
        @Index(columnList = "fileId")
})
public class LogLineInfo extends BaseEntity {

    /**
     * 所属的uid
     */
    @Column
    public String uid;

    /**
     * 调用深度
     */
    @Column
    public Integer depth;

    /**
     * 关联的日志文件
     */
    @Column(nullable = false)
    public Integer fileId;

    /**
     * 该行日志的时间
     */
    @Column(nullable = false)
    public Date time;

    /**
     * 对应文件的开始字节位置
     */
    @Column(nullable = false)
    public Long fromByte;

    /**
     * 对应文件的结束字节位置
     */
    @Column(nullable = false)
    public Long toByte;
}
