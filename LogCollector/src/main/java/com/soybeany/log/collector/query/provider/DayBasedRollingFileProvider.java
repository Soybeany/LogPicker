package com.soybeany.log.collector.query.provider;

import com.soybeany.log.collector.query.data.QueryParam;
import com.soybeany.log.core.model.LogException;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/3/23
 */
public class DayBasedRollingFileProvider implements FileProvider {

    private static final Pattern LOG_FILE_TIME_PATTERN = Pattern.compile("<\\?(.+)\\?>");

    private final String dirToScan;
    private final String logTodayFileName;
    private final String logHistoryFileName;
    private int maxFilesToQuery = 10;

    /**
     * @param dirToScan          待扫描的目录
     * @param logTodayFileName   当天日志文件的命名
     * @param logHistoryFileName 历史日志文件的命名，使用<?TimeFormat?>作时间占位
     */
    public DayBasedRollingFileProvider(String dirToScan, String logTodayFileName, String logHistoryFileName) {
        this.dirToScan = dirToScan;
        this.logTodayFileName = logTodayFileName;
        this.logHistoryFileName = logHistoryFileName;
    }

    @Override
    public Set<File> onGetFiles(QueryParam param) {
        // 若已指定日志文件，则不需再添加
        if (!param.getLogFiles().isEmpty()) {
            return Collections.emptySet();
        }
        // 补充日志文件
        LocalDate fDate = param.getFromTime().toLocalDate();
        LocalDate tDate = param.getToTime().toLocalDate();
        LocalDate today = LocalDate.now();
        LocalDate tempDate = fDate;
        LinkedHashSet<File> logFiles = new LinkedHashSet<>();
        while (!tempDate.isAfter(tDate)) {
            addFiles(logFiles, today, tempDate);
            tempDate = tempDate.plusDays(1);
        }
        int size = logFiles.size();
        if (size > maxFilesToQuery) {
            throw new LogException("一次查询最多允许" + maxFilesToQuery + "个文件，当前为" + size + "个");
        }
        return logFiles;
    }

    /**
     * 配置单次最多能查询的文件数
     */
    public DayBasedRollingFileProvider maxFilesToQuery(int count) {
        maxFilesToQuery = count;
        return this;
    }

    // ********************内部方法********************

    private void addFiles(LinkedHashSet<File> logFiles, LocalDate today, LocalDate date) {
        if (today.isEqual(date)) {
            addFiles(logFiles, logTodayFileName);
        } else {
            addFiles(logFiles, toFileName(logHistoryFileName, date));
        }
    }

    private void addFiles(LinkedHashSet<File> logFiles, String fileName) {
        File file = new File(dirToScan, fileName);
        if (file.exists()) {
            logFiles.add(file);
        }
    }

    private String toFileName(String template, LocalDate date) {
        Matcher matcher = LOG_FILE_TIME_PATTERN.matcher(template);
        if (!matcher.find()) {
            return template;
        }
        String timeString = date.format(DateTimeFormatter.ofPattern(matcher.group(1)));
        return matcher.replaceAll(timeString);
    }

}
