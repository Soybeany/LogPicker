package com.soybeany.log.collector.service.common;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.core.model.FileRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/20
 */
public interface BytesRangeService {

    /**
     * 拼接
     */
    void append(LinkedList<FileRange> ranges, long fromByte, long toByte);

    /**
     * 合并范围
     */
    LinkedList<FileRange> merge(LinkedList<FileRange> ranges);

    /**
     * 计算出多个指定范围列表的交集
     */
    @NonNull
    List<FileRange> intersect(List<List<FileRange>> rangeList);

}

@Service
class BytesRangeServiceImpl implements BytesRangeService {

    @Autowired
    private AppConfig appConfig;

    @Override
    public void append(LinkedList<FileRange> ranges, long fromByte, long toByte) {
        FileRange lastRange = ranges.peekLast();
        // 存在范围且差距小于指定值，延长结束下标
        if (null != lastRange && (toByte - lastRange.to <= appConfig.maxBytesGapToMerge)) {
            lastRange.to = toByte;
            return;
        }
        // 其余情况，创建新范围
        ranges.add(new FileRange(fromByte, toByte));
    }

    @Override
    public LinkedList<FileRange> merge(LinkedList<FileRange> ranges) {
        // 转换为位点
        List<Point> points = new ArrayList<>();
        for (FileRange range : ranges) {
            points.add(new Point(range.from, true));
            points.add(new Point(range.to, false));
        }
        // 排序
        Collections.sort(points);
        // 得到范围
        LinkedList<FileRange> result = new LinkedList<>();
        int curLevel = 0;
        Point lastStartPoint = null;
        for (Point point : points) {
            if (point.isStart) {
                FileRange lastRange = result.peekLast();
                if (null != lastRange && (point.index - lastRange.to <= appConfig.maxBytesGapToMerge)) {
                    result.pollLast();
                } else if (curLevel == 0) {
                    lastStartPoint = point;
                }
                curLevel++;
                continue;
            }
            if (--curLevel == 0 && null != lastStartPoint && lastStartPoint.index != point.index) {
                result.add(new FileRange(lastStartPoint.index, point.index));
            }
        }
        return result;
    }

    @Override
    public List<FileRange> intersect(List<List<FileRange>> rangeList) {
        // 转换为位点
        List<Point> points = new ArrayList<>();
        for (List<FileRange> ranges : rangeList) {
            for (FileRange range : ranges) {
                points.add(new Point(range.from, true));
                points.add(new Point(range.to, false));
            }
        }
        // 排序
        Collections.sort(points);
        // 得到范围
        List<FileRange> result = new LinkedList<>();
        int targetLevel = rangeList.size(), curLevel = 0;
        Point lastStartPoint = null;
        for (Point point : points) {
            // 若为起始点，值积累
            if (point.isStart) {
                lastStartPoint = point;
                curLevel++;
                continue;
            }
            // 如果达到目标值，则进行记录
            if (curLevel == targetLevel && null != lastStartPoint && lastStartPoint.index != point.index) {
                result.add(new FileRange(lastStartPoint.index, point.index));
            }
            // 结束点，降一级level
            curLevel--;
        }
        return result;
    }

    private static class Point implements Comparable<Point> {
        long index;
        boolean isStart;

        public Point(long index, boolean isStart) {
            this.index = index;
            this.isStart = isStart;
        }

        @Override
        public int compareTo(Point o) {
            if (index > o.index) {
                return 1;
            } else if (index < o.index) {
                return -1;
            }
            return 0;
        }
    }
}