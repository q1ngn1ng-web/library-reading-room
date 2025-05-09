package com.example.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class ServerConfig {
    @Autowired
    private HttpServletRequest request;

    public String getFileBaseUrl() {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/files/";
    }
}

