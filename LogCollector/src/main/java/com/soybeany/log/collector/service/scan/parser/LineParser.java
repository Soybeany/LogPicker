package com.soybeany.log.collector.service.scan.parser;

import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogLine;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public interface LineParser {

    /**
     * @return 能够解析返回对象，不能解析返回null
     */
    @Nullable
    LogLine parse(String lineString);

}

@Scope("prototype")
@Component
class LineParserImpl implements LineParser {

    private final String timeRegex = "(?<time>.{10} .{8})\\.\\d{3}";
    private final String levelRegex = "(?<level>\\w+)";
    private final String uidRegex = "(?<uid>\\w+)";
    private final String threadRegex = "\\[(?<thread>.+)]";
    private final String contentRegex = "(?<content>.+)";
    private final Pattern linePattern = Pattern.compile(timeRegex + "\\s+" + levelRegex + "\\s+" + uidRegex + "\\s+---\\s+" + threadRegex + "\\s+:\\s+" + contentRegex);

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

    @Override
    public LogLine parse(String lineString) {
        Matcher matcher = linePattern.matcher(lineString);
        if (!matcher.find()) {
            return null;
        }
        LogLine line = new LogLine();
        try {
            line.time = dateFormat.parse(matcher.group("time"));
        } catch (ParseException e) {
            throw new LogException("时间解析异常:" + e.getMessage());
        }
        line.uid = matcher.group("uid");
        line.thread = matcher.group("thread");
        line.level = matcher.group("level");
        line.content = matcher.group("content");
        return line;
    }
}