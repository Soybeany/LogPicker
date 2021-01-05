package com.soybeany.log.collector.repository;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Date;

/**
 * 标签信息
 *
 * @author Soybeany
 * @date 2021/1/4
 */
@Entity
@Table(indexes = {
        @Index(columnList = "uid"),
        @Index(columnList = "fileId"),
        @Index(name = "key_time_index", columnList = "key"),
        @Index(name = "key_time_index", columnList = "time"),
        @Index(name = "key_uid_index", columnList = "key"),
        @Index(name = "key_uid_index", columnList = "uid"),
})
public class TagInfo extends BaseEntity {

    /**
     * 创建该标记的时间
     */
    @Column(nullable = false)
    public Date time;

    /**
     * 所属的uid
     */
    @Column(nullable = false)
    public String uid;

    /**
     * 关联的日志文件
     */
    @Column(nullable = false)
    public Integer fileId;

    /**
     * 标签的键
     */
    @Column(nullable = false)
    public String key;

    /**
     * 标签的值
     */
    @Column(nullable = false)
    public String value;

}
