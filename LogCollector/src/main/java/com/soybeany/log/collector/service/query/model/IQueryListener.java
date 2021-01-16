package com.soybeany.log.collector.service.query.model;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public interface IQueryListener extends Comparable<IQueryListener> {

    @Override
    default int compareTo(IQueryListener o) {
        return o.priority() - priority();
    }

    /**
     * 优先级，值越大，越先被执行，默认值为0
     */
    default int priority() {
        return 0;
    }

    default void onQuery(QueryContext context) {
    }

    default void onHandleNextContext(QueryContext old, QueryContext next) {
    }

}
