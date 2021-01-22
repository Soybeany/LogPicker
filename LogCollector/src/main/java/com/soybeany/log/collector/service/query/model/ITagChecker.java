package com.soybeany.log.collector.service.query.model;

import com.soybeany.log.core.model.LogPack;

/**
 * @author Soybeany
 * @date 2021/1/22
 */
public interface ITagChecker {

    ITagChecker WITHOUT = new ITagChecker() {
    };

    default boolean containsAllTags(QueryParam param, LogPack logPack) {
        return true;
    }

}
