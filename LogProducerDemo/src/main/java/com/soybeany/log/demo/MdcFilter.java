package com.soybeany.log.demo;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Soybeany
 * @date 2020/12/31
 */
@Component
public class MdcFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 如果请求中有uuid，则使用指定的uuid；缺失再创建新的uuid
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(18) + ".0";
        try {
            MDC.put("xxx", uuid);
            chain.doFilter(request, response);
        } finally {
            MDC.remove("xxx");
        }
    }
}
