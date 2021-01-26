package com.soybeany.log.collector.service.common;

import com.soybeany.util.file.BdFileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/18
 */
class LogLoaderServiceImplTest {

    private Pattern linePattern = Pattern.compile("^\\[(?<time>.{17})] \\[(?<level>INFO|WARN|ERROR)] \\[(?<thread>.+?)] \\{(?<pos>.+?)}-(?<content>.*)");
    private Pattern tagPattern = Pattern.compile("^FLAG-(?<type>.+?)-(?<state>.+?):(?<user>.+?) (?<url>.+?) (?<param>\\{.+})");

    @Test
    void load() throws Exception {
        long start = System.currentTimeMillis();
        File file = new File("E:\\logDir2\\sysAudit.log.2018-10-15.log");
        BdFileUtils.randomReadLine(file, 0, new BdFileUtils.RandomReadLineCallback() {
            @Override
            public String onSetupCharset() {
                return "GBK";
            }

            @Override
            public int onHandleLine(long from, long to, String line) throws IOException {
                Matcher matcher = linePattern.matcher(line);
                if (!matcher.find()) {
                    return 0;
                }
                Matcher matcher1 = tagPattern.matcher(matcher.group("content"));
                matcher1.find();
                return 0;
            }
        });
        System.out.println("耗时:" + (System.currentTimeMillis() - start) / 1000 + "s");
    }

}