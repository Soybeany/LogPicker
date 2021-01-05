package com.soybeany.log.collector.repository;

import javax.persistence.*;

/**
 * 基础实体
 *
 * @author Soybeany
 * @date 2020/7/28
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    @Column(nullable = false)
    private Integer version;

    public Integer getId() {
        return id;
    }

    public Integer getVersion() {
        return version;
    }

    public boolean isNew() {
        return null == id;
    }

}
