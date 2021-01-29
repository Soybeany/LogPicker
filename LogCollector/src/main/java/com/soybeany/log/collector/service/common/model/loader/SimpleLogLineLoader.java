package com.soybeany.log.collector.service.common.model.loader;

import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogLine;
import com.soybeany.log.core.model.LogTag;
import com.soybeany.util.file.BufferedRandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.soybeany.log.core.model.Constants.*;

/**
 * @author Soybeany
 * @date 2021/1/24
 */
public class SimpleLogLineLoader implements ILogLineLoader {

    private final RandomAccessFile raf;
    private final String charSet;
    private final Pattern linePattern;
    private final Pattern tagPattern;

    private final LastLogLineHolder lastLogLineHolder = new LastLogLineHolder();
    private long nextFromByte;
    private long readPointer;

    public SimpleLogLineLoader(File file, String charset, Pattern linePattern, Pattern tagPattern) throws IOException {
        if (!file.exists()) {
            throw new LogException("找不到名称为“" + file.getName() + "”的日志文件");
        }
        this.raf = new BufferedRandomAccessFile(file, "r");
        this.charSet = charset;
        this.linePattern = linePattern;
        this.tagPattern = tagPattern;
    }

    // ********************静态方法********************

    private static LogLine parseStringToLogLine(Pattern pattern, String lineString) {
        Matcher matcher = pattern.matcher(lineString);
        if (!matcher.find()) {
            return null;
        }
        LogLine logLine = new LogLine();
        logLine.time = matcher.group(PARSER_KEY_TIME);
        // todo 待核实uid是否能为null
        logLine.uid = matcher.group(PARSER_KEY_UID);
        logLine.thread = matcher.group(PARSER_KEY_THREAD);
        logLine.level = matcher.group(PARSER_KEY_LEVEL);
        logLine.content = matcher.group(PARSER_KEY_CONTENT);
        return logLine;
    }

    private static LogTag parseLogLineToLogTag(Pattern pattern, LogLine logLine) {
        Matcher matcher = pattern.matcher(logLine.content);
        if (!matcher.find()) {
            return null;
        }
        LogTag tag = new LogTag();
        tag.uid = logLine.uid;
        tag.thread = logLine.thread;
        tag.time = logLine.time;
        tag.key = matcher.group(PARSER_KEY_KEY);
        tag.value = matcher.group(PARSER_KEY_VALUE);
        return tag;
    }

    // ********************公开方法********************

    @Override
    public boolean loadNextLogLine(ResultHolder resultHolder) throws IOException {
        while (true) {
            String line = raf.readLine();
            // 若没有读到新的行，则直接返回
            if (null == line) {
                return popLastLogLine(resultHolder);
            }
            // 行解析
            String lineString = new String(line.getBytes(StandardCharsets.ISO_8859_1), charSet);
            long toByte = raf.getFilePointer();
            LogLine curLogLine = parseStringToLogLine(linePattern, lineString);
            // 读取完整的一行
            if (null == curLogLine) {
                appendLogToLastLogLine(toByte, lineString);
                continue;
            }
            // 还没有上一行，则继续读取
            if (null == lastLogLineHolder.logLine) {
                updateLastHolderAndNextFrom(toByte, curLogLine);
                continue;
            }
            // 设置结果
            setupResult(resultHolder);
            // 更新holder
            updateLastHolderAndNextFrom(toByte, curLogLine);
            return true;
        }
    }

    @Override
    public long getReadPointer() {
        return readPointer;
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    public void seek(long pointer) throws IOException {
        raf.seek(nextFromByte = pointer);
        lastLogLineHolder.reset();
        readPointer = 0;
    }

    // ********************内部方法********************

    private void updateLastHolderAndNextFrom(long toByte, LogLine logLine) {
        lastLogLineHolder.update(nextFromByte, toByte, logLine);
        nextFromByte = toByte;
    }

    private void appendLogToLastLogLine(long toByte, String lineString) {
        // 舍弃无法拼接的日志
        if (null == lastLogLineHolder.logLine) {
            return;
        }
        lastLogLineHolder.append(toByte, lineString);
    }

    private boolean popLastLogLine(ResultHolder resultHolder) {
        // 若已没有上一行，直接返回
        if (null == lastLogLineHolder.logLine) {
            return false;
        }
        // 返回并重置
        setupResult(resultHolder);
        lastLogLineHolder.reset();
        return true;
    }

    private void setupResult(ResultHolder resultHolder) {
        lastLogLineHolder.mergeTempContent();
        resultHolder.fromByte = lastLogLineHolder.fromByte;
        resultHolder.toByte = readPointer = lastLogLineHolder.toByte;
        resultHolder.logLine = lastLogLineHolder.logLine;
        resultHolder.logTag = parseLogLineToLogTag(tagPattern, resultHolder.logLine);
        resultHolder.isTag = (null != resultHolder.logTag);
    }

    // ********************内部类********************

    private static class LastLogLineHolder {
        LogLine logLine;
        StringBuilder tempContent;
        long fromByte;
        long toByte;

        void append(long toByte, String lineString) {
            if (null == tempContent) {
                tempContent = new StringBuilder(logLine.content);
            }
            tempContent.append(lineString);
            this.toByte = toByte;
        }

        void update(long fromByte, long toByte, LogLine logLine) {
            this.fromByte = fromByte;
            this.toByte = toByte;
            this.logLine = logLine;
            tempContent = null;
        }

        void reset() {
            update(0, 0, null);
        }

        void mergeTempContent() {
            if (null == tempContent) {
                return;
            }
            logLine.content = tempContent.toString();
            tempContent = null;
        }
    }

}
