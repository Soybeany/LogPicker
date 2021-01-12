package com.soybeany.log.collector.service.scan.parser;

import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/12
 */
@Scope("prototype")
@Component
public class CompactLogParser implements LogParser {

    private final Map<String, String> uidMap = new HashMap<>();

    @Override
    public LogLine parseToLogLine(Pattern pattern, String lineString) {
        return null;
    }

    @Override
    public List<LogTag> parseToLogTags(LogLine logLine) {

        return null;
    }

    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("(?<time>.{10} .{8})\\.\\d{3}\\s+(?<level>\\w+)\\s+(?<uid>\\S+)\\s+---\\s+\\[\\s*(?<thread>.+)]\\s+:\\s+(?<content>.+)");
        String msg = "2020-12-31 16:04:54.986  INFO 78e68065e1ac3d.0 --- [nio-8282-exec-1] : 调用成功";
        String msg2 = "2020-12-31 16:04:54.969  INFO          unknown --- [nio-8282-exec-1] : Completed initialization in 5 ms";
        Matcher matcher = pattern.matcher(msg);
        System.out.println(matcher.find());
    }

}
