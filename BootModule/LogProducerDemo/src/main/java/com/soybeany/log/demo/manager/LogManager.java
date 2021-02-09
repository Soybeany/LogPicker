package com.soybeany.log.demo.manager;

import com.soybeany.log.core.model.Direction;

/**
 * @author Soybeany
 * @date 2021/2/5
 */
public class LogManager {

    public static QueryExecutor query() {
        return new QueryExecutor();
    }

    public static String help() {
        return Direction.QUERY;
    }

}