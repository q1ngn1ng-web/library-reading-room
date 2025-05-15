package com.example.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class ServerConfig {
    @Autowired
    private HttpServletRequest request;

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    public String getFileBaseUrl() {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/files/";
    }

    /**
     * 获取文件上传路径
     */
    public String getUploadPath() {
        return uploadPath;
    }

    /**
     * 获取服务器URL
     */
    public String getUrl() {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
}
