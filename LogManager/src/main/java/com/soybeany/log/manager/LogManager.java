package com.soybeany.log.manager;

import com.soybeany.log.core.model.Direction;
import com.soybeany.log.manager.executor.QueryExecutor;

import java.util.LinkedHashMap;

/**
 * @author Soybeany
 * @date 2021/2/5
 */
public class LogManager {

    public static QueryManager query(QueryExecutor queryExecutor, UrlProvider defaultUrlProvider, int maxResultCount) {
        return new QueryManager(queryExecutor, defaultUrlProvider, maxResultCount);
    }

    public static String queryHelp() {
        Direction direction = new Direction();
        return direction.defaultIntroduce()
                .defaultParams()
                .defaultResults()
                .defaultSpecials()
                .defaultTimeSupports()
                .addOthers("管理参数", new LinkedHashMap<String, String>() {{
                    put("logSearchHosts", "使用指定参数同时查询的服务器，加上请求路径");
                    put("uidSearchHosts", "需要日志关联查询(使用uid)的服务器，适用于关联分布式/微服务架构服务器上的日志");
                    put("hideMsg", "是否需要隐藏查询情况信息");
                }}).build();
    }

}