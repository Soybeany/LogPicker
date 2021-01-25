package com.soybeany.log.collector.service.common.model.loader;

import com.soybeany.log.collector.service.common.data.FileRange;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/25
 */
public class RangesLogLineLoader implements ILogLineLoader {

    private static final long DEFAULT_POINTER = -1;

    private final SimpleLogLineLoader delegate;
    private final List<FileRange> ranges;
    private int rangeIndex;
    private long targetPointer = DEFAULT_POINTER;
    private long readPointer = DEFAULT_POINTER;

    public RangesLogLineLoader(File file, String charset, Pattern linePattern, Pattern tagPattern, List<FileRange> ranges) throws IOException {
        this.delegate = new SimpleLogLineLoader(file, charset, linePattern, tagPattern);
        this.ranges = ranges;
    }

    @Override
    public boolean loadNextLogLine(ResultHolder resultHolder) throws IOException {
        while (true) {
            // 检查范围
            boolean endOfRange = adjustPointer();
            if (endOfRange) {
                return false;
            }
            // 没有下一行则返回
            if (!delegate.loadNextLogLine(resultHolder)) {
                return false;
            }
            // 已超出当前范围，则进入下一范围查找
            readPointer = resultHolder.toByte;
            if (targetPointer <= readPointer) {
                continue;
            }
            return true;
        }
    }

    @Override
    public long getReadPointer() throws IOException {
        return delegate.getReadPointer();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    // ********************内部方法********************

    /**
     * @return 是否已达全部范围的结尾
     */
    private boolean adjustPointer() throws IOException {
        // 若未到达当前范围的末尾，则不作处理
        if (readPointer < targetPointer) {
            return false;
        }
        // 若无更多的范围，则返回EOR
        if (++rangeIndex >= ranges.size()) {
            return true;
        }
        // 切换到下一范围
        readPointer = DEFAULT_POINTER;
        FileRange newRange = ranges.get(rangeIndex);
        targetPointer = newRange.to;
        delegate.seek(newRange.from);
        return false;
    }

}
