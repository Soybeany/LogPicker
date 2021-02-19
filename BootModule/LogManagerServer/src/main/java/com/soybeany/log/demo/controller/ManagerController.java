package com.soybeany.log.demo.controller;

import com.soybeany.log.demo.config.AppConfig;
import com.soybeany.log.manager.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/2/5
 */
@RestController
public class ManagerController {

    private final Map<String, IAction> actionMap = new HashMap<>();

    @Autowired
    private AppConfig appConfig;

    @RequestMapping("/**")
    public String forDirectRead(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, String> param) {
        String uri = request.getRequestURI();
        String path = uri.substring(request.getContextPath().length());
        IAction action = actionMap.get(path);
        if (null == action) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        response.setContentType(action.onGetContentType());
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        try {
            return action.onInvoke(headers, param);
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @PostConstruct
    private void init() {
        actionMap.put(appConfig.queryPath, new IAction() {
            @Override
            public String onInvoke(Map<String, String> headers, Map<String, String> param) {
                return LogManager.query().getResult(appConfig.outerQueryPath, headers, param, appConfig.resultRetainSec);
            }

            @Override
            public String onGetContentType() {
                return MediaType.APPLICATION_JSON_VALUE;
            }
        });
        actionMap.put(appConfig.helpPath, (headers, param) -> LogManager.queryHelp(appConfig.helpPath, appConfig.queryPath));
    }

    private interface IAction {
        String onInvoke(Map<String, String> headers, Map<String, String> param);

        default String onGetContentType() {
            return MediaType.TEXT_PLAIN_VALUE;
        }
    }

}
