//package com.soybeany.log.collector.service.query.filter;
//
//import com.soybeany.log.collector.service.common.model.LogFilter;
//import com.soybeany.log.collector.service.query.data.QueryContext;
//import com.soybeany.log.core.model.LogLine;
//import com.soybeany.log.core.model.LogPack;
//import org.springframework.stereotype.Component;
//
///**
// * @author Soybeany
// * @date 2021/1/5
// */
//@Component
//class KeyContainsLogFilterFactory implements LogFilterFactory {
//
//    private static final String P_KEY_CONTAINS_KEY = "containsKey";
//
//    @Override
//    public LogFilter getNewLogFilterIfInNeed(QueryContext context) {
//        String key = context.getParam(PREFIX, P_KEY_CONTAINS_KEY);
//        // 若没有配置，则不作过滤
//        if (null == key) {
//            return null;
//        }
//        // 返回新的过滤器
//        return new FilterImpl(key);
//    }
//
//    // ********************内部类********************
//
//    private static class FilterImpl implements LogFilter {
//
//        private final String key;
//
//        public FilterImpl(String key) {
//            this.key = key;
//        }
//
//        @Override
//        public boolean filterLogPack(LogPack logPack) {
//            for (LogLine logLine : logPack.logLines) {
//                if (logLine.content.contains(key)) {
//                    return false;
//                }
//            }
//            // 否则进行过滤
//            return true;
//        }
//    }
//}
