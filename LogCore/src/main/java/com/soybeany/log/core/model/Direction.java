package com.soybeany.log.core.model;

/**
 * @author Soybeany
 * @date 2021/2/8
 */
public interface Direction {

    static String query(String helpPath, String forDirectReadPath) {
        return ""
                + "查询建议"
                + "\n  1.指定时间查询区间"
                + "\n  2.使用索引型的标签参数"
                + "\n  3.按需设置结果条数限制"
                + "\n\n可用接口"
                + "\n  " + helpPath + "(GET): 提供帮助，也就是当前接口"
                + "\n  " + forDirectReadPath + "(POST): 使用指定参数自由查询，并返回可直接阅读的数据(json)"
                + "\n\n标准参数"
                + "\n  fromTime: 开始时间，支持下方所列的多种时间格式(默认当天00:00)"
                + "\n  toTime: 结束时间，支持下方所列的多种时间格式(默认当前时间)"
                + "\n  countLimit: 一次查询最多返回的结果条数(默认使用配置值)"
                + "\n  logFiles: 指定待查询的文件全路径，如“D:\\logs\\xxx.log”，多个时使用“;”或“,”进行分隔(默认根据时间选择文件)"
                + "\n  uidList: 指定待查询的uid，多个时使用“;”或“,”进行分隔(默认不指定)"
                + "\n  resultId: 查询指定id的结果，用于分页查询(默认不指定)"
                + "\n\n标签参数"
                + "\n  格式为“tag-xxx”，如“tag-url”、“tag-user”等，分为“索引型”与“过滤型”两种，在查询结果的msg中能查看"
                + "\n  索引型：扫描时会索引该tag，查询前使用索引锁定查询范围，可极大地提高查询速度"
                + "\n  过滤型：参考过滤参数，不能提高查询速度(支持同时指定多个)"
                + "\n  匹配策略为contain，不区分大小写"
                + "\n\n过滤参数"
                + "\n  格式为“filter-xxx”，如“filter-containsKey”，查询后才对该结果进行条件筛选(支持同时指定多个)"
                + "\n  containsKey: key包含，不区分大小写"
                + "\n\n支持的时间格式"
                + "\n  1.yyyy-MM-dd HH:mm:ss，最完整"
                + "\n  2.yyyy-MM-dd HH:mm，不含秒"
                + "\n  3.yy-MM-dd HH:mm:ss，简写"
                + "\n  4.yy-MM-dd HH:mm，简写，不含秒"
                + "\n  5.HH:mm:ss，日期为今天"
                + "\n  6.HH:mm，日期为今天，不含秒"
                + "\n\n常见问题"
                + "\n  1.存在分页的情况下，下一页结果的时间可能比上一页的早：匹配时是按索引记录的顺序进行的，也就是a记录处理完后，再处理b记录（b的出现时间比a早，但索引顺序在a后）"
                ;
    }

}
