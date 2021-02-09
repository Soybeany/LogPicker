package com.soybeany.log.core.model;

/**
 * @author Soybeany
 * @date 2021/2/8
 */
public interface Direction {

    String QUERY = ""
            + "查询建议"
            + "\n  1.指定时间查询区间"
            + "\n  2.使用索引型的标签参数"
            + "\n  3.按需设置结果条数限制"
            + "\n\n可用接口"
            + "\n  /query/help: 提供帮助，也就是当前接口"
            + "\n  /query/byParam: 使用指定参数自由查询"
            + "\n  /query/forDirectRead: 与/byParam一致，只是返回的数据为可直接阅读的数据，为json字符串(重要，常用)"
            + "\n  /query/forPack: 与/byParam一致，只是返回的数据为打包数据，为json字符串"
            + "\n  /query/forRaw: 与/byParam一致，只是返回的数据为原始数据，为序列化字符串"
            + "\n\n标准参数"
            + "\n  fromTime: 开始时间，支持下方所列的多种时间格式(默认当天00:00)"
            + "\n  toTime: 结束时间，支持下方所列的多种时间格式(默认当前时间)"
            + "\n  countLimit: 一次查询最多返回的结果条数(默认使用配置值)"
            + "\n  logFiles: 指定待查询的文件全路径，如“D:\\logs\\xxx.log”，多个时使用“;”或“,”进行分隔(默认根据时间选择文件)"
            + "\n  uidList: 指定待查询的uid，多个时使用“;”或“,”进行分隔(默认不指定)"
            + "\n  result-id: 查询指定id的结果，用于分页查询(默认不指定)"
            + "\n\n标签参数"
            + "\n  格式为“tag-xxx”，如“tag-url”、“tag-user”等，分为“索引型”与“过滤型”两种，在查询结果的msg中能查看"
            + "\n  索引型：扫描时会索引该tag，查询前使用索引锁定查询范围，可极大地提高查询速度"
            + "\n  过滤型：参考过滤参数，不能提高查询速度"
            + "\n  匹配策略为contain，不区分大小写"
            + "\n\n过滤参数"
            + "\n  格式为“filter-xxx”，如“filter-containsKey”，查询后才对该结果进行条件筛选"
            + "\n  containsKey: key包含，不区分大小写"
            + "\n\n支持的时间格式"
            + "\n  1.yyyy-MM-dd HH:mm:ss，最完整"
            + "\n  2.yyyy-MM-dd HH:mm，不含秒"
            + "\n  3.yy-MM-dd HH:mm:ss，简写"
            + "\n  4.yy-MM-dd HH:mm，简写，不含秒"
            + "\n  5.HH:mm:ss，日期为今天"
            + "\n  6.HH:mm，日期为今天，不含秒";

}
