package com.soybeany.log.manager;

import com.soybeany.log.core.model.Direction;
import com.soybeany.log.manager.executor.QueryExecutor;

/**
 * @author Soybeany
 * @date 2021/2/5
 */
public class LogManager {

    public static QueryManager query(QueryExecutor queryExecutor, UrlProvider defaultUrlProvider, int maxResultCount) {
        return new QueryManager(queryExecutor, defaultUrlProvider, maxResultCount);
    }

    public static String queryHelp(String helpPath, String forDirectReadPath) {
        return Direction.query(helpPath, forDirectReadPath) + "\n\n" + getManagerHelpDirection();
    }

    // ********************内部方法********************

    private static String getManagerHelpDirection() {
        return ""
                + "管理参数"
                + "\n  logSearchUrls: 使用指定参数同时查询的服务器，相当于3.0的serverIps，加上请求路径"
                + "\n  uidSearchUrls: 需要日志关联查询(使用uid)的服务器，适用于关联分布式/微服务架构服务器上的日志"
                + "\n  hideMsg: 是否需要隐藏查询情况信息，相当于3.0的hideUnimportantInfo"
                ;
    }

}