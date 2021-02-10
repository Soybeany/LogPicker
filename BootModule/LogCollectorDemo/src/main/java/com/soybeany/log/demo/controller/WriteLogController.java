package com.soybeany.log.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2020/12/31
 */
@RestController
public
class WriteLogController {

    private final Logger LOG = LoggerFactory.getLogger(String.class);

    @GetMapping("/test")
    String test() {
        LOG.info("调用成功");
        LOG.warn("调用成功{}{}", "好", "你");
        LOG.error("调用成功3", new RuntimeException("测试"));

        new Thread(new RunnableWrapper(() -> {
            try {
                LOG.info("线程开始");
                Thread.sleep(2000);
                LOG.info("线程结束");
            } catch (Exception e) {
            }
        })).start();
        return "success";
    }

    private static class RunnableWrapper implements Runnable {

        private final Map<String, String> mContextMap;
        private final Runnable mTarget;

        public RunnableWrapper(Runnable target) {
            mContextMap = MDC.getCopyOfContextMap();
            this.mTarget = target;
        }

        @Override
        public void run() {
            MDC.setContextMap(mContextMap);
            mTarget.run();
        }
    }

}
