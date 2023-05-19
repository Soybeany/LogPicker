package com.soybeany.log.collector.common;

import com.soybeany.log.collector.query.exporter.DirectReadLogExporter;
import com.soybeany.util.file.BdFileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    @Test
    public void test() throws Exception {
        LogQueryService service = new LogQueryService();
        Map<String, String[]> param = new HashMap<>();
        param.put("tag-url", new String[]{"querytasklistv2"});
        param.put("fromTime", new String[]{"2023-05-19 10:22:41"});
        param.put("logFiles", new String[]{"D:/sysAudit.log"});
        String result = service.query(param, new DirectReadLogExporter());
        service.stop();
    }

    @Test
    public void test2() throws IOException {
//        String text;
//        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
//            BdFileUtils.randomRead(new File("D:/sysAudit.log"), os, 19900, 19996);
//            text = os.toString("GBK");
//        }
        BdFileUtils.randomReadLine(new File("D:/sysAudit.log"), 14679, (l, l1, s) -> {
            System.out.println(s);
            return 0;
        });
        System.out.println("good");
    }

}