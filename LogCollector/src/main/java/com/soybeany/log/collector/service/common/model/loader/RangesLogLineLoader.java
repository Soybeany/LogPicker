package com.soybeany.log.collector.service.common.model.loader;

import com.soybeany.log.core.model.FileRange;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/25
 */
public class RangesLogLineLoader implements ILogLineLoader {

    private final SimpleLogLineLoader delegate;
    private List<? extends FileRange> ranges;
    private int rangeIndex;
    private long targetPointer;

    public RangesLogLineLoader(File file, String charset, Pattern linePattern, Pattern tagPattern) throws IOException {
        this.delegate = new SimpleLogLineLoader(file, charset, linePattern, tagPattern);
    }

    public void switchRanges(List<? extends FileRange> ranges) {
        this.ranges = ranges;
        rangeIndex = -1;
        targetPointer = -1;
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
            if (targetPointer < resultHolder.toByte) {
                continue;
            }
            return true;
        }
    }

    @Override
    public long getReadPointer() {
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
        if (getReadPointer() < targetPointer) {
            return false;
        }
        // 若无更多的范围，则返回EOR
        if (++rangeIndex >= ranges.size()) {
            return true;
        }
        // 切换到下一范围
        FileRange newRange = ranges.get(rangeIndex);
        targetPointer = newRange.to;
        delegate.seek(newRange.from);
        return false;
    }

}
