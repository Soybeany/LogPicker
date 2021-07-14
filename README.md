# LogPicker(日志提取工具)
### 接入指引
* 在pom中引入依赖(版本号只作参考，请使用当前最新版本)
	* 自定义仓库(必选)
`<repository>
    <id>jitpack.io</id>
    <url>https://www.jitpack.io</url>
</repository>`

	* 核心模块（必选）
`<dependency>
<groupId>com.github.Soybeany.LogPicker</groupId>
<artifactId>LogCollector</artifactId>
<version>4.3.15</version>
</dependency>`

	* 标签模块（可选）
`<dependency>
   <groupId>com.github.Soybeany.LogPicker</groupId>
   <artifactId>LogTagWriter</artifactId>
   <version>4.3.15</version>
</dependency>`

* 定义配置(下载源码后，每个属性有说明)
* 在@PostConstruct时初始化并创建一个QueryService单例
* 提供“查询日志”的api
* 提供“帮助”的api
* 标签模块继承TagWriteListener，可按需重写几个protect的方法
* 调用“帮助”接口，查看使用说明，并开始日志查询
